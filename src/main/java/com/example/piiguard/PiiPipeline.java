package com.example.piiguard;

import com.example.piiguard.config.AppConfig;
import com.example.piiguard.config.EnvUtils;
import com.example.piiguard.ingestion.RecordParserFn;
import com.example.piiguard.monitoring.ElasticsearchBulkWriteFn;
import com.example.piiguard.processing.DetectAndAnonymizeFn;
import com.example.piiguard.processing.PiiProcessingTags;
import com.example.piiguard.processing.PiiRecord;
import com.example.piiguard.storage.PostgresWriter;
import java.util.ArrayList;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.aws2.options.AwsOptions;
import org.apache.beam.sdk.io.aws2.options.S3Options;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.GroupIntoBatches;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.WithKeys;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TupleTagList;
import org.joda.time.Duration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * Builds the Apache Beam pipeline for PII processing.
 */
public final class PiiPipeline {
  private static final TupleTag<PiiRecord> MAIN_TAG = new TupleTag<>() {
  };

  private PiiPipeline() {
  }

  /**
   * Builds a Beam pipeline from configuration.
   *
   * @param config application configuration
   * @return pipeline instance
   */
  public static Pipeline build(AppConfig config) {
    var options = createPipelineOptions(config);
    return build(config, options);
  }

  /**
   * Builds a Beam pipeline using provided options.
   *
   * @param config application configuration
   * @param options pipeline options
   * @return pipeline instance
   */
  public static Pipeline build(AppConfig config, PipelineOptions options) {
    configureAws(options, config);

    var pipeline = Pipeline.create(options);

    var skipHeaders = config.getInput().getSkipHeaderLines();
    var read = TextIO.read().from(config.getInput().getFilePattern());
    if (skipHeaders != null && skipHeaders > 0) {
      read = read.withSkipHeaderLines(skipHeaders);
    }

    var lines = pipeline.apply("ReadInput", read);
    var records = lines.apply("ParseRecords", ParDo.of(new RecordParserFn(config.getInput())))
        .setCoder(SerializableCoder.of(com.example.piiguard.ingestion.InputRecord.class));

    var tuple = records.apply("DetectAndAnonymize",
        ParDo.of(new DetectAndAnonymizeFn(config.getPresidio(), config.getApp().getName()))
            .withOutputTags(MAIN_TAG, TupleTagList.of(PiiProcessingTags.LOGS).and(PiiProcessingTags.METRICS)));

    var processed = tuple.get(MAIN_TAG);
    writeToPostgres(processed, config);

    if (config.getMonitoring() != null && config.getMonitoring().isLogsEnabled()) {
      writeLogs(tuple, config);
    }

    if (config.getMonitoring() != null && config.getMonitoring().isMetricsEnabled()) {
      writeMetrics(tuple, config);
    }

    return pipeline;
  }

  /**
   * Creates pipeline options using config-provided runner settings.
   *
   * @param config application configuration
   * @return pipeline options
   */
  private static PipelineOptions createPipelineOptions(AppConfig config) {
    var args = new ArrayList<String>();
    var beam = config.getBeam();
    if (beam != null) {
      if (beam.getRunner() != null && !beam.getRunner().isBlank()) {
        args.add("--runner=" + beam.getRunner());
      }
      if (beam.getTempLocation() != null && !beam.getTempLocation().isBlank()) {
        args.add("--tempLocation=" + beam.getTempLocation());
      }
      if (beam.getStagingLocation() != null && !beam.getStagingLocation().isBlank()) {
        args.add("--stagingLocation=" + beam.getStagingLocation());
      }
      if (beam.getJobName() != null && !beam.getJobName().isBlank()) {
        args.add("--jobName=" + beam.getJobName());
      }
    }
    return PipelineOptionsFactory.fromArgs(args.toArray(String[]::new)).create();
  }

  /**
   * Configures AWS/S3 options for S3-compatible storage.
   *
   * @param options Beam pipeline options
   * @param config application configuration
   */
  private static void configureAws(PipelineOptions options, AppConfig config) {
    if (config.getS3() == null) {
      return;
    }
    var s3 = config.getS3();
    var awsOptions = options.as(AwsOptions.class);
    if (s3.getRegion() != null && !s3.getRegion().isBlank()) {
      awsOptions.setAwsRegion(Region.of(s3.getRegion()));
    }
    if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
      awsOptions.setEndpoint(java.net.URI.create(s3.getEndpoint()));
    }

    var accessKey = EnvUtils.getEnvOrDefault(s3.getAccessKeyEnv(), "");
    var secretKey = EnvUtils.getEnvOrDefault(s3.getSecretKeyEnv(), "");
    if (!accessKey.isBlank() && !secretKey.isBlank()) {
      awsOptions.setAwsCredentialsProvider(StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKey, secretKey)));
    }

    System.setProperty("s3.path.style.access", String.valueOf(s3.isPathStyleAccess()));
    var s3Options = options.as(S3Options.class);
    s3Options.setS3ClientFactoryClass(com.example.piiguard.util.CustomS3ClientBuilderFactory.class);
  }

  /**
   * Writes processed records to Postgres.
   *
   * @param processed processed record collection
   * @param config application configuration
   */
  private static void writeToPostgres(PCollection<PiiRecord> processed, AppConfig config) {
    var postgres = config.getPostgres();
    var username = EnvUtils.getRequiredEnv(postgres.getUserEnv());
    var password = EnvUtils.getRequiredEnv(postgres.getPasswordEnv());

    processed.apply("WriteToPostgres",
        PostgresWriter.build(postgres.getJdbcUrl(), username, password, postgres.getTable(), postgres.getBatchSize()));
  }

  /**
   * Writes log events to Elasticsearch.
   *
   * @param tuple processing outputs
   * @param config application configuration
   */
  private static void writeLogs(PCollectionTuple tuple, AppConfig config) {
    var settings = config.getElasticsearch();
    var logs = tuple.get(PiiProcessingTags.LOGS)
        .setCoder(SerializableCoder.of(com.example.piiguard.monitoring.LogEvent.class));

    logs.apply("LogKeys", WithKeys.of("logs"))
        .setCoder(KvCoder.of(StringUtf8Coder.of(), logs.getCoder()))
        .apply("BatchLogs", GroupIntoBatches.<String, com.example.piiguard.monitoring.LogEvent>ofSize(
            settings.getBulkSize())
            .withMaxBufferingDuration(Duration.standardSeconds(settings.getBulkFlushSeconds())))
        .apply("WriteLogs", ParDo.of(new ElasticsearchBulkWriteFn<>(settings, settings.getLogsIndexPrefix())));
  }

  /**
   * Writes metric events to Elasticsearch.
   *
   * @param tuple processing outputs
   * @param config application configuration
   */
  private static void writeMetrics(PCollectionTuple tuple, AppConfig config) {
    var settings = config.getElasticsearch();
    var metrics = tuple.get(PiiProcessingTags.METRICS)
        .setCoder(SerializableCoder.of(com.example.piiguard.monitoring.MetricEvent.class));

    metrics.apply("MetricKeys", WithKeys.of("metrics"))
        .setCoder(KvCoder.of(StringUtf8Coder.of(), metrics.getCoder()))
        .apply("BatchMetrics", GroupIntoBatches.<String, com.example.piiguard.monitoring.MetricEvent>ofSize(
            settings.getBulkSize())
            .withMaxBufferingDuration(Duration.standardSeconds(settings.getBulkFlushSeconds())))
        .apply("WriteMetrics", ParDo.of(new ElasticsearchBulkWriteFn<>(settings, settings.getMetricsIndexPrefix())));
  }
}
