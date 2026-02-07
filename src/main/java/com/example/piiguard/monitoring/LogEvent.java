package com.example.piiguard.monitoring;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Log event for bulk indexing into Elasticsearch.
 */
public class LogEvent implements Serializable {
  public Instant timestamp;
  public String level;
  public String message;
  public String recordId;
  public int piiCount;
  public List<String> entityTypes;
  public String pipeline;
  public String error;

  /** Default constructor for Beam. */
  public LogEvent() {
  }

  /**
   * Creates a log event.
   *
   * @param timestamp event timestamp
   * @param level log level
   * @param message log message
   * @param recordId record id
   * @param piiCount pii count
   * @param entityTypes entity types
   * @param pipeline pipeline name
   * @param error error message
   */
  public LogEvent(Instant timestamp, String level, String message, String recordId, int piiCount,
                  List<String> entityTypes, String pipeline, String error) {
    this.timestamp = timestamp;
    this.level = level;
    this.message = message;
    this.recordId = recordId;
    this.piiCount = piiCount;
    this.entityTypes = entityTypes;
    this.pipeline = pipeline;
    this.error = error;
  }
}
