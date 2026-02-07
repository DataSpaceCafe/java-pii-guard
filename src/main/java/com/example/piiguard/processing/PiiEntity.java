package com.example.piiguard.processing;

import java.io.Serializable;

/**
 * Represents a detected PII entity span.
 */
public class PiiEntity implements Serializable {
  public int start;
  public int end;
  public double score;
  public String entityType;

  /** Default constructor for Beam. */
  public PiiEntity() {
  }

  /**
   * Creates a PII entity span.
   *
   * @param start start offset
   * @param end end offset
   * @param score confidence score
   * @param entityType entity type
   */
  public PiiEntity(int start, int end, double score, String entityType) {
    this.start = start;
    this.end = end;
    this.score = score;
    this.entityType = entityType;
  }
}
