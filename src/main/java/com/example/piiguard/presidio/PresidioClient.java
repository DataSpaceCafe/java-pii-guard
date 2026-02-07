package com.example.piiguard.presidio;

import com.example.piiguard.config.AppConfig;
import com.example.piiguard.processing.PiiEntity;
import com.example.piiguard.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal Presidio REST client for analyzer and anonymizer services.
 */
public class PresidioClient implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(PresidioClient.class);

  private final HttpClient client;
  private final URI analyzerUri;
  private final URI anonymizerUri;

  /**
   * Creates a client using analyzer/anonymizer URLs.
   *
   * @param analyzerUrl analyzer base URL
   * @param anonymizerUrl anonymizer base URL
   */
  public PresidioClient(String analyzerUrl, String anonymizerUrl) {
    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    this.analyzerUri = URI.create(stripTrailingSlash(analyzerUrl) + "/analyze");
    this.anonymizerUri = URI.create(stripTrailingSlash(anonymizerUrl) + "/anonymize");
  }

  /**
   * Calls Presidio Analyzer API.
   *
   * @param text input text
   * @param settings presidio settings
   * @return list of detected entities
   */
  public List<PiiEntity> analyze(String text, AppConfig.PresidioSettings settings) {
    var payload = new HashMap<String, Object>();
    payload.put("text", text);
    payload.put("language", settings.getLanguage());
    payload.put("score_threshold", settings.getScoreThreshold());
    if (settings.getEntities() != null && !settings.getEntities().isEmpty()) {
      payload.put("entities", settings.getEntities());
    }

    var adHoc = buildAdHocRecognizers(settings);
    if (!adHoc.isEmpty()) {
      payload.put("ad_hoc_recognizers", adHoc);
    }

    var body = JsonUtils.toJson(payload);
    var request = HttpRequest.newBuilder(analyzerUri)
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        LOGGER.warn("Analyzer request failed: {} - {}", response.statusCode(), response.body());
        return List.of();
      }
      var results = JsonUtils.mapper().readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
      });
      var entities = new ArrayList<PiiEntity>();
      for (var result : results) {
        var start = asInt(result.get("start"));
        var end = asInt(result.get("end"));
        var score = asDouble(result.get("score"));
        var entityType = String.valueOf(result.get("entity_type"));
        entities.add(new PiiEntity(start, end, score, entityType));
      }
      return entities;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Analyzer request interrupted", e);
      return List.of();
    } catch (IOException e) {
      LOGGER.warn("Analyzer request failed", e);
      return List.of();
    }
  }

  /**
   * Calls Presidio Anonymizer API.
   *
   * @param text input text
   * @param entities analyzer results
   * @param anonymizers anonymizer operator map
   * @return anonymized text
   */
  public String anonymize(String text, List<PiiEntity> entities, Map<String, Map<String, Object>> anonymizers) {
    var payload = new HashMap<String, Object>();
    payload.put("text", text);
    payload.put("analyzer_results", toAnalyzerResults(entities));
    if (anonymizers != null && !anonymizers.isEmpty()) {
      payload.put("anonymizers", anonymizers);
    }

    var body = JsonUtils.toJson(payload);
    var request = HttpRequest.newBuilder(anonymizerUri)
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 300) {
        LOGGER.warn("Anonymizer request failed: {} - {}", response.statusCode(), response.body());
        return text;
      }
      var result = JsonUtils.mapper().readValue(response.body(), new TypeReference<Map<String, Object>>() {
      });
      var anonymizedText = result.get("text");
      return anonymizedText == null ? text : String.valueOf(anonymizedText);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Anonymizer request interrupted", e);
      return text;
    } catch (IOException e) {
      LOGGER.warn("Anonymizer request failed", e);
      return text;
    }
  }

  /**
   * Builds ad-hoc regex recognizers for Presidio Analyzer.
   *
   * @param settings presidio settings
   * @return recognizer payload list
   */
  private List<Map<String, Object>> buildAdHocRecognizers(AppConfig.PresidioSettings settings) {
    var recognizers = settings.getCustomRecognizers();
    if (recognizers == null || recognizers.isEmpty()) {
      return List.of();
    }
    var result = new ArrayList<Map<String, Object>>();
    for (var recognizer : recognizers) {
      var recognizerMap = new HashMap<String, Object>();
      recognizerMap.put("name", recognizer.getName());
      recognizerMap.put("supported_entity", recognizer.getSupportedEntity());
      recognizerMap.put("supported_language", settings.getLanguage());
      var pattern = new HashMap<String, Object>();
      pattern.put("name", recognizer.getName());
      pattern.put("regex", recognizer.getRegex());
      pattern.put("score", recognizer.getScore());
      if (recognizer.getFlags() != null && !recognizer.getFlags().isBlank()) {
        pattern.put("flags", recognizer.getFlags());
      }
      recognizerMap.put("patterns", List.of(pattern));
      result.add(recognizerMap);
    }
    return result;
  }

  /**
   * Converts entities into Presidio analyzer results payload.
   *
   * @param entities detected entities
   * @return analyzer results list
   */
  private List<Map<String, Object>> toAnalyzerResults(List<PiiEntity> entities) {
    var results = new ArrayList<Map<String, Object>>();
    if (entities == null) {
      return results;
    }
    for (var entity : entities) {
      var map = new HashMap<String, Object>();
      map.put("start", entity.start);
      map.put("end", entity.end);
      map.put("score", entity.score);
      map.put("entity_type", entity.entityType);
      results.add(map);
    }
    return results;
  }

  /**
   * Converts a numeric object to int.
   *
   * @param value numeric value
   * @return int value
   */
  private int asInt(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return Integer.parseInt(String.valueOf(value));
  }

  /**
   * Converts a numeric object to double.
   *
   * @param value numeric value
   * @return double value
   */
  private double asDouble(Object value) {
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return Double.parseDouble(String.valueOf(value));
  }

  /**
   * Strips trailing slash from a URL string.
   *
   * @param value input string
   * @return string without trailing slash
   */
  private String stripTrailingSlash(String value) {
    if (value == null) {
      return "";
    }
    if (value.endsWith("/")) {
      return value.substring(0, value.length() - 1);
    }
    return value;
  }

  /**
   * Closes the client.
   */
  @Override
  public void close() {
    // HttpClient has no explicit close; method kept for symmetry.
  }
}
