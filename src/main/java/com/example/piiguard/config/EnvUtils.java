package com.example.piiguard.config;

/**
 * Utility for reading environment variables with defaults.
 */
public final class EnvUtils {
  private EnvUtils() {
  }

  /**
   * Reads an environment variable or returns the default value. System properties are checked first.
   *
   * @param envName environment variable name
   * @param defaultValue default value when env is missing or blank
   * @return resolved value
   */
  public static String getEnvOrDefault(String envName, String defaultValue) {
    if (envName == null || envName.isBlank()) {
      return defaultValue;
    }
    var value = System.getProperty(envName);
    if (value == null || value.isBlank()) {
      value = System.getenv(envName);
    }
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }

  /**
   * Reads an environment variable and throws if missing. System properties are checked first.
   *
   * @param envName environment variable name
   * @return value
   */
  public static String getRequiredEnv(String envName) {
    if (envName == null || envName.isBlank()) {
      throw new IllegalArgumentException("Environment variable name is blank");
    }
    var value = System.getProperty(envName);
    if (value == null || value.isBlank()) {
      value = System.getenv(envName);
    }
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing environment variable: " + envName);
    }
    return value;
  }
}
