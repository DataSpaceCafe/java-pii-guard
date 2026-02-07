package com.example.piiguard.presidio;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper to build anonymizer configuration with salt for hash operators.
 */
public final class PresidioAnonymizerConfig {
  private PresidioAnonymizerConfig() {
  }

  /**
   * Returns an anonymizer configuration with salt applied to hash operators.
   *
   * @param base base anonymizer config
   * @param salt salt string
   * @return anonymizer config with salt injected
   */
  public static Map<String, Map<String, Object>> withSalt(Map<String, Map<String, Object>> base, String salt) {
    var result = new HashMap<String, Map<String, Object>>();
    if (base == null || base.isEmpty()) {
      return result;
    }
    var saltValue = salt == null ? "" : salt;
    for (var entry : base.entrySet()) {
      var operator = new HashMap<String, Object>();
      if (entry.getValue() != null) {
        operator.putAll(entry.getValue());
      }
      if (!saltValue.isBlank()) {
        var type = operator.get("type");
        if (type != null && "hash".equalsIgnoreCase(String.valueOf(type)) && !operator.containsKey("salt")) {
          operator.put("salt", saltValue);
        }
      }
      result.put(entry.getKey(), operator);
    }
    return result;
  }
}
