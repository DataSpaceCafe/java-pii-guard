package com.example.piiguard;

import com.example.piiguard.config.ConfigLoader;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point for the PII pipeline.
 */
public final class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

  private App() {
  }

  /**
   * Starts the pipeline using the provided configuration.
   *
   * @param args command-line args
   */
  public static void main(String[] args) {
    try {
      var configPath = resolveConfigPath(args);
      var config = ConfigLoader.load(Path.of(configPath));
      LOGGER.info("Starting pipeline with config {}", configPath);
      var pipeline = PiiPipeline.build(config);
      pipeline.run().waitUntilFinish();
    } catch (Exception e) {
      LOGGER.error("Failed to start pipeline", e);
      System.exit(1);
    }
  }

  /**
   * Resolves configuration path from args or CONFIG_PATH env.
   *
   * @param args command-line args
   * @return config path
   */
  private static String resolveConfigPath(String[] args) {
    var envPath = System.getenv("CONFIG_PATH");
    var configPath = envPath == null || envPath.isBlank() ? "config/config.yaml" : envPath;
    if (args == null) {
      return configPath;
    }
    for (var i = 0; i < args.length; i++) {
      var arg = args[i];
      if (arg.startsWith("--config=")) {
        return arg.substring("--config=".length());
      }
      if ("--config".equals(arg) && i + 1 < args.length) {
        return args[i + 1];
      }
    }
    return configPath;
  }
}
