package com.example.piiguard.processing;

import com.example.piiguard.config.AppConfig;
import com.example.piiguard.config.EnvUtils;
import com.example.piiguard.ingestion.InputRecord;
import com.example.piiguard.monitoring.LogEvent;
import com.example.piiguard.monitoring.MetricEvent;
import com.example.piiguard.presidio.PresidioClient;
import com.example.piiguard.presidio.PresidioAnonymizerConfig;
import com.example.piiguard.util.JsonUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects PII with Presidio Analyzer and anonymizes using Presidio Anonymizer.
 */
public class DetectAndAnonymizeFn extends DoFn<InputRecord, PiiRecord> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DetectAndAnonymizeFn.class);

  private final AppConfig.PresidioSettings presidioSettings;
  private final String pipelineName;

  private transient PresidioClient client;

  /**
   * Creates a DetectAndAnonymizeFn.
   *
   * @param presidioSettings presidio config
   * @param pipelineName pipeline name
   */
  public DetectAndAnonymizeFn(AppConfig.PresidioSettings presidioSettings, String pipelineName) {
    this.presidioSettings = presidioSettings;
    this.pipelineName = pipelineName;
  }

  /** Initializes Presidio client. */
  @Setup
  public void setup() {
    this.client = new PresidioClient(presidioSettings.getAnalyzerUrl(), presidioSettings.getAnonymizerUrl());
  }

  /**
   * Processes each record and emits anonymized output, logs, and metrics.
   *
   * @param context Beam process context
   */
  @ProcessElement
  public void processElement(ProcessContext context) {
    var record = context.element();
    var start = System.nanoTime();
    try {
      var entities = client.analyze(record.text, presidioSettings);
      var filtered = PiiEntityFilter.filterOverlaps(entities);

      var salt = EnvUtils.getEnvOrDefault(presidioSettings.getHashSaltEnv(), presidioSettings.getHashSalt());
      var anonymizers = PresidioAnonymizerConfig.withSalt(presidioSettings.getAnonymizers(), salt);

      var anonymized = client.anonymize(record.text, filtered, anonymizers);
      var entityTypes = filtered.stream()
          .map(entity -> entity.entityType)
          .distinct()
          .collect(Collectors.toList());
      var entitiesJson = JsonUtils.toJson(filtered);

      var output = new PiiRecord(
          record.recordId,
          record.source,
          record.text,
          anonymized,
          filtered.size(),
          entityTypes,
          entitiesJson,
          Instant.now(),
          true,
          null);

      context.output(output);
      emitLog(context, record.recordId, filtered.size(), entityTypes, null, "Processed record");
      emitMetrics(context, record.recordId, filtered.size(), start, true);
    } catch (Exception e) {
      LOGGER.warn("Failed to process record {}", record.recordId, e);
      var output = new PiiRecord(
          record.recordId,
          record.source,
          record.text,
          record.text,
          0,
          List.of(),
          "[]",
          Instant.now(),
          false,
          e.getMessage());
      context.output(output);
      emitLog(context, record.recordId, 0, List.of(), e.getMessage(), "Processing failed");
      emitMetrics(context, record.recordId, 0, start, false);
    }
  }

  private void emitLog(ProcessContext context, String recordId, int piiCount, List<String> entityTypes,
                       String error, String message) {
    var event = new LogEvent(Instant.now(), error == null ? "INFO" : "ERROR", message,
        recordId, piiCount, entityTypes, pipelineName, error);
    context.output(PiiProcessingTags.LOGS, event);
  }

  /**
   * Emits metrics for PII count and processing duration.
   *
   * @param context Beam context
   * @param recordId record id
   * @param piiCount detected entity count
   * @param startNanos start time in nanos
   * @param success success flag
   */
  private void emitMetrics(ProcessContext context, String recordId, int piiCount, long startNanos, boolean success) {
    var durationMs = (System.nanoTime() - startNanos) / 1_000_000.0;
    var tags = Map.of("success", String.valueOf(success));
    var countMetric = new MetricEvent(Instant.now(), "pii_count", piiCount, "count", recordId, pipelineName, tags);
    var durationMetric = new MetricEvent(Instant.now(), "processing_ms", durationMs, "ms", recordId, pipelineName, tags);
    context.output(PiiProcessingTags.METRICS, countMetric);
    context.output(PiiProcessingTags.METRICS, durationMetric);
  }

  /** Closes Presidio client. */
  @Teardown
  public void teardown() {
    if (client != null) {
      client.close();
    }
  }
}
