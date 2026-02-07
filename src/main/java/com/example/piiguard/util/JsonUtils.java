package com.example.piiguard.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON helper utilities.
 */
public final class JsonUtils {
  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

  private JsonUtils() {
  }

  /**
   * Serializes an object to JSON string.
   *
   * @param value object to serialize
   * @return JSON string
   */
  public static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize JSON", e);
    }
  }

  /**
   * Returns the shared ObjectMapper.
   *
   * @return object mapper
   */
  public static ObjectMapper mapper() {
    return MAPPER;
  }
}
