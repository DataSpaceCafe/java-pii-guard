package com.example.piiguard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads configuration from YAML file.
 */
public final class ConfigLoader {
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
      .registerModule(new JavaTimeModule());

  private ConfigLoader() {
  }

  /**
   * Loads configuration from a YAML file path.
   *
   * @param path YAML path
   * @return parsed AppConfig
   * @throws IOException when reading fails
   */
  public static AppConfig load(Path path) throws IOException {
    try (var input = Files.newInputStream(path)) {
      return MAPPER.readValue(input, AppConfig.class);
    }
  }
}
