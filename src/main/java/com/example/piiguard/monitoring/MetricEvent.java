package com.example.piiguard.monitoring;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Metric event for bulk indexing into Elasticsearch.
 */
public class MetricEvent implements Serializable {
  public Instant timestamp;
  public String name;
  public double value;
  public String unit;
  public String recordId;
  public String pipeline;
  public Map<String, String> tags;

  /** Default constructor for Beam. */
  public MetricEvent() {
  }

  /**
   * Creates a metric event.
   *
   * @param timestamp event timestamp
   * @param name metric name
   * @param value metric value
   * @param unit metric unit
   * @param recordId record id
   * @param pipeline pipeline name
   * @param tags metric tags
   */
  public MetricEvent(Instant timestamp, String name, double value, String unit, String recordId,
                     String pipeline, Map<String, String> tags) {
    this.timestamp = timestamp;
    this.name = name;
    this.value = value;
    this.unit = unit;
    this.recordId = recordId;
    this.pipeline = pipeline;
    this.tags = tags;
  }
}
