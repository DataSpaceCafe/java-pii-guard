package com.example.piiguard.ingestion;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Input record parsed from CSV/JSON sources.
 */
public class InputRecord implements Serializable {
  public String recordId;
  public String source;
  public String text;
  public Map<String, Object> attributes;
  public Instant receivedAt;

  /** Default constructor for Beam serialization. */
  public InputRecord() {
  }

  /**
   * Creates a new input record.
   *
   * @param recordId record identifier
   * @param source source label
   * @param text text to analyze
   * @param attributes raw fields
   * @param receivedAt ingestion time
   */
  public InputRecord(String recordId, String source, String text, Map<String, Object> attributes, Instant receivedAt) {
    this.recordId = recordId;
    this.source = source;
    this.text = text;
    this.attributes = attributes;
    this.receivedAt = receivedAt;
  }
}
