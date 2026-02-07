package com.example.piiguard.monitoring;

import com.example.piiguard.config.AppConfig;
import com.example.piiguard.config.EnvUtils;
import com.example.piiguard.util.JsonUtils;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Beam DoFn that performs bulk indexing into Elasticsearch.
 *
 * @param <T> event type
 */
public class ElasticsearchBulkWriteFn<T> extends DoFn<KV<String, Iterable<T>>, Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchBulkWriteFn.class);
  private static final DateTimeFormatter INDEX_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd")
      .withZone(ZoneOffset.UTC);

  private final AppConfig.ElasticsearchSettings settings;
  private final String indexPrefix;

  private transient RestClient restClient;

  /**
   * Creates a bulk writer DoFn.
   *
   * @param settings elasticsearch settings
   * @param indexPrefix index prefix
   */
  public ElasticsearchBulkWriteFn(AppConfig.ElasticsearchSettings settings, String indexPrefix) {
    this.settings = settings;
    this.indexPrefix = indexPrefix;
  }

  /** Initializes Elasticsearch REST client. */
  @Setup
  public void setup() {
    var username = EnvUtils.getEnvOrDefault(settings.getUsernameEnv(), "");
    var password = EnvUtils.getEnvOrDefault(settings.getPasswordEnv(), "");
    this.restClient = ElasticsearchClientFactory.create(settings.getUrl(), username, password);
  }

  /**
   * Sends a batch to Elasticsearch bulk API.
   *
   * @param context Beam context
   */
  @ProcessElement
  public void processElement(ProcessContext context) {
    var events = context.element().getValue();
    var payload = buildBulkPayload(events);
    if (payload.isEmpty()) {
      return;
    }

    var request = new Request("POST", "/_bulk");
    request.setEntity(new NStringEntity(payload, ContentType.create("application/x-ndjson")));

    try {
      var response = restClient.performRequest(request);
      if (response.getStatusLine().getStatusCode() >= 300) {
        LOGGER.warn("Bulk indexing failed: {}", response.getStatusLine());
      }
    } catch (Exception e) {
      LOGGER.warn("Bulk indexing failed", e);
    }
  }

  /**
   * Builds an NDJSON bulk payload for Elasticsearch.
   *
   * @param events events to serialize
   * @return NDJSON payload
   */
  private String buildBulkPayload(Iterable<T> events) {
    var documents = new ArrayList<String>();
    for (var event : events) {
      documents.add(JsonUtils.toJson(event));
    }
    if (documents.isEmpty()) {
      return "";
    }
    var indexName = indexPrefix + "-" + INDEX_DATE.format(Instant.now());
    var builder = new StringBuilder();
    for (var doc : documents) {
      builder.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}")
          .append("\n");
      builder.append(doc).append("\n");
    }
    return builder.toString();
  }

  /** Closes the REST client. */
  @Teardown
  public void teardown() {
    if (restClient != null) {
      try {
        restClient.close();
      } catch (Exception e) {
        LOGGER.debug("Failed to close Elasticsearch client", e);
      }
    }
  }
}
