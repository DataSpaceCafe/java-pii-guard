package com.example.piiguard.processing;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Output record containing anonymized text and detection metadata.
 */
public class PiiRecord implements Serializable {
  public String recordId;
  public String source;
  public String originalText;
  public String anonymizedText;
  public int piiCount;
  public List<String> entityTypes;
  public String entitiesJson;
  public Instant processedAt;
  public boolean success;
  public String errorMessage;

  /** Default constructor for Beam. */
  public PiiRecord() {
  }

  /**
   * Creates a new output record.
   *
   * @param recordId record identifier
   * @param source source label
   * @param originalText original text
   * @param anonymizedText anonymized text
   * @param piiCount number of entities
   * @param entityTypes list of entity types
   * @param entitiesJson serialized entity list
   * @param processedAt processing timestamp
   * @param success success flag
   * @param errorMessage error message
   */
  public PiiRecord(String recordId, String source, String originalText, String anonymizedText,
                   int piiCount, List<String> entityTypes, String entitiesJson, Instant processedAt,
                   boolean success, String errorMessage) {
    this.recordId = recordId;
    this.source = source;
    this.originalText = originalText;
    this.anonymizedText = anonymizedText;
    this.piiCount = piiCount;
    this.entityTypes = entityTypes;
    this.entitiesJson = entitiesJson;
    this.processedAt = processedAt;
    this.success = success;
    this.errorMessage = errorMessage;
  }
}
