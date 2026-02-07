package com.example.piiguard.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * Tests for configuration loading.
 */
class ConfigLoaderTest {
  /**
   * Verifies YAML parsing works for core fields.
   */
  @Test
  void loadsYamlConfig() throws Exception {
    var yaml = """
        app:\n  name: test-app\n  environment: test\n        input:\n  format: csv\n  filePattern: s3://bucket/*.csv\n  textField: text\n        s3:\n  endpoint: http://localhost:9000\n  region: us-east-1\n  pathStyleAccess: true\n        presidio:\n  analyzerUrl: http://localhost:5001\n  anonymizerUrl: http://localhost:5002\n  language: en\n  scoreThreshold: 0.5\n        postgres:\n  jdbcUrl: jdbc:postgresql://localhost:5432/pii\n  table: pii_records\n  userEnv: POSTGRES_USER\n  passwordEnv: POSTGRES_PASSWORD\n        elasticsearch:\n  url: http://localhost:9200\n  logsIndexPrefix: ppi-logs\n  metricsIndexPrefix: ppi-metrics\n  usernameEnv: ELASTIC_USERNAME\n  passwordEnv: ELASTIC_PASSWORD\n        monitoring:\n  logsEnabled: true\n  metricsEnabled: true\n        """;
    var temp = Files.createTempFile("config", ".yaml");
    Files.writeString(temp, yaml);

    var config = ConfigLoader.load(temp);
    assertThat(config.getApp().getName()).isEqualTo("test-app");
    assertThat(config.getInput().getFormat()).isEqualTo("csv");
    assertThat(config.getS3().isPathStyleAccess()).isTrue();
  }
}
