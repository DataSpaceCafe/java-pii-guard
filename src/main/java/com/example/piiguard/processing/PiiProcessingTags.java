package com.example.piiguard.processing;

import com.example.piiguard.monitoring.LogEvent;
import com.example.piiguard.monitoring.MetricEvent;
import org.apache.beam.sdk.values.TupleTag;

/**
 * TupleTags for side outputs from the PII processing step.
 */
public final class PiiProcessingTags {
  /** Side output tag for log events. */
  public static final TupleTag<LogEvent> LOGS = new TupleTag<>() {
  };

  /** Side output tag for metric events. */
  public static final TupleTag<MetricEvent> METRICS = new TupleTag<>() {
  };

  private PiiProcessingTags() {
  }
}
