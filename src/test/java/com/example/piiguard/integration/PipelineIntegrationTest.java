package com.example.piiguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.piiguard.PiiPipeline;
import com.example.piiguard.config.AppConfig;
import com.example.piiguard.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.PostgreSQLContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * End-to-end pipeline test using Testcontainers.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PipelineIntegrationTest {
  private static final String BUCKET = "pii-bucket";
  private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

  @Container
  private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  private static final ElasticsearchContainer ELASTIC = new ElasticsearchContainer(
      DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.12.2"))
      .withEnv("xpack.security.enabled", "false")
      .withEnv("xpack.security.http.ssl.enabled", "false")
      .withEnv("discovery.type", "single-node")
      .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

  @Container
  private static final MinIOContainer MINIO = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
      .withUserName("minio")
      .withPassword("minio123");

  private HttpServer presidioServer;

  /**
   * Starts dependencies and uploads sample data.
   */
  @BeforeAll
  void setUp() throws Exception {
    startPresidioMock();
    createSchema();
    uploadSampleData();
    System.setProperty("POSTGRES_USER", POSTGRES.getUsername());
    System.setProperty("POSTGRES_PASSWORD", POSTGRES.getPassword());
    System.setProperty("MINIO_ACCESS_KEY", MINIO.getUserName());
    System.setProperty("MINIO_SECRET_KEY", MINIO.getPassword());
    System.setProperty("S3_PATH_STYLE_ACCESS", "true");
  }

  /**
   * Stops mock Presidio server.
   */
  @AfterAll
  void tearDown() {
    if (presidioServer != null) {
      presidioServer.stop(0);
    }
  }

  /**
   * Runs the pipeline and verifies database and Elasticsearch outputs.
   */
  @Test
  void runsPipelineEndToEnd() throws Exception {
    var config = buildConfig();
    var options = PipelineOptionsFactory.create();

    PipelineResult result = PiiPipeline.build(config, options).run();
    result.waitUntilFinish();

    verifyPostgres();
    verifyElasticsearch();
  }

  /**
   * Starts a lightweight mock Presidio server.
   *
   * @throws IOException when the server cannot start
   */
  private void startPresidioMock() throws IOException {
    presidioServer = HttpServer.create(new InetSocketAddress(0), 0);
    presidioServer.createContext("/analyze", this::handleAnalyze);
    presidioServer.createContext("/anonymize", this::handleAnonymize);
    presidioServer.start();
  }

  /**
   * Handles mock Presidio analyze requests.
   *
   * @param exchange HTTP exchange
   * @throws IOException when response write fails
   */
  private void handleAnalyze(HttpExchange exchange) throws IOException {
    var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    var payload = JsonUtils.mapper().readValue(body, new TypeReference<Map<String, Object>>() {
    });
    var text = String.valueOf(payload.get("text"));
    var matcher = EMAIL_PATTERN.matcher(text);
    var results = new java.util.ArrayList<Map<String, Object>>();
    while (matcher.find()) {
      results.add(Map.of(
          "start", matcher.start(),
          "end", matcher.end(),
          "score", 0.85,
          "entity_type", "EMAIL_ADDRESS"));
    }
    var response = JsonUtils.toJson(results);
    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
    exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
    exchange.close();
  }

  /**
   * Handles mock Presidio anonymize requests.
   *
   * @param exchange HTTP exchange
   * @throws IOException when response write fails
   */
  private void handleAnonymize(HttpExchange exchange) throws IOException {
    var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    var payload = JsonUtils.mapper().readValue(body, new TypeReference<Map<String, Object>>() {
    });
    var text = String.valueOf(payload.get("text"));
    var response = JsonUtils.toJson(Map.of("text", text.replaceAll(EMAIL_PATTERN.pattern(), "<HASHED>")));
    exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
    exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
    exchange.close();
  }

  /**
   * Uploads sample CSV data into MinIO.
   */
  private void uploadSampleData() {
    var endpoint = MINIO.getS3URL();
    var credentials = AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword());
    var s3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();

    try {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    } catch (S3Exception e) {
      // ignore if already exists
    }

    var csv = "record_id,text,source\n1,Contact me at alice@example.com,unit-test\n";
    s3Client.putObject(
        PutObjectRequest.builder().bucket(BUCKET).key("input/test.csv").build(),
        RequestBody.fromString(csv));
  }

  /**
   * Creates Postgres schema for output records.
   *
   * @throws Exception when SQL execution fails
   */
  private void createSchema() throws Exception {
    var jdbcUrl = POSTGRES.getJdbcUrl();
    try (var connection = DriverManager.getConnection(jdbcUrl, POSTGRES.getUsername(), POSTGRES.getPassword());
         var statement = connection.createStatement()) {
      statement.executeUpdate("""
          CREATE TABLE IF NOT EXISTS pii_records (
            record_id VARCHAR(255) PRIMARY KEY,
            source VARCHAR(255),
            original_text TEXT,
            anonymized_text TEXT,
            pii_count INTEGER,
            entity_types TEXT,
            entities_json JSONB,
            processed_at TIMESTAMP,
            success BOOLEAN,
            error_message TEXT
          );
          """);
    }
  }

  /**
   * Builds configuration for integration testing.
   *
   * @return app config
   */
  private AppConfig buildConfig() {
    var app = new AppConfig.AppSettings();
    app.setName("pii-test");
    app.setEnvironment("test");
    app.setLogLevel("INFO");

    var input = new AppConfig.InputSettings();
    input.setFormat("csv");
    input.setFilePattern("s3://" + BUCKET + "/input/test.csv");
    input.setTextField("text");
    input.setIdField("record_id");
    input.setSkipHeaderLines(1);
    input.setCsvDelimiter(",");
    input.setCsvHeaders(List.of("record_id", "text", "source"));

    var s3 = new AppConfig.S3Settings();
    s3.setEndpoint(MINIO.getS3URL());
    s3.setRegion("us-east-1");
    s3.setPathStyleAccess(true);
    s3.setAccessKeyEnv("MINIO_ACCESS_KEY");
    s3.setSecretKeyEnv("MINIO_SECRET_KEY");

    var presidio = new AppConfig.PresidioSettings();
    var baseUrl = "http://localhost:" + presidioServer.getAddress().getPort();
    presidio.setAnalyzerUrl(baseUrl);
    presidio.setAnonymizerUrl(baseUrl);
    presidio.setLanguage("en");
    presidio.setScoreThreshold(0.5);
    presidio.setEntities(List.of("EMAIL_ADDRESS"));
    presidio.setHashSaltEnv("PII_HASH_SALT");
    presidio.setHashSalt("PLACEHOLDER_SALT");
    presidio.setAnonymizers(Map.of(
        "DEFAULT", Map.of("type", "redact"),
        "EMAIL_ADDRESS", Map.of("type", "hash", "hash_type", "sha256")));

    var postgres = new AppConfig.PostgresSettings();
    postgres.setJdbcUrl(POSTGRES.getJdbcUrl());
    postgres.setTable("pii_records");
    postgres.setUserEnv("POSTGRES_USER");
    postgres.setPasswordEnv("POSTGRES_PASSWORD");
    postgres.setBatchSize(100);

    var elastic = new AppConfig.ElasticsearchSettings();
    elastic.setUrl("http://" + ELASTIC.getHttpHostAddress());
    elastic.setLogsIndexPrefix("ppi-logs");
    elastic.setMetricsIndexPrefix("ppi-metrics");
    elastic.setBulkSize(10);
    elastic.setBulkFlushSeconds(1);

    var monitoring = new AppConfig.MonitoringSettings();
    monitoring.setLogsEnabled(true);
    monitoring.setMetricsEnabled(true);

    var beam = new AppConfig.BeamSettings();
    beam.setRunner("DirectRunner");

    var config = new AppConfig();
    config.setApp(app);
    config.setInput(input);
    config.setS3(s3);
    config.setPresidio(presidio);
    config.setPostgres(postgres);
    config.setElasticsearch(elastic);
    config.setMonitoring(monitoring);
    config.setBeam(beam);
    return config;
  }

  /**
   * Verifies records were inserted into Postgres.
   *
   * @throws Exception when query fails
   */
  private void verifyPostgres() throws Exception {
    try (var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
         var statement = connection.createStatement()) {
      var resultSet = statement.executeQuery("SELECT COUNT(*) FROM pii_records");
      resultSet.next();
      var count = resultSet.getInt(1);
      assertThat(count).isEqualTo(1);
    }
  }

  /**
   * Verifies log documents are present in Elasticsearch.
   *
   * @throws Exception when HTTP request fails
   */
  private void verifyElasticsearch() throws Exception {
    var httpClient = HttpClient.newHttpClient();
    var countUri = URI.create("http://" + ELASTIC.getHttpHostAddress() + "/ppi-logs-*/_count");

    var request = HttpRequest.newBuilder(countUri).timeout(Duration.ofSeconds(10)).GET().build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var payload = JsonUtils.mapper().readValue(response.body(), new TypeReference<Map<String, Object>>() {
    });
    var count = ((Number) payload.get("count")).intValue();
    assertThat(count).isGreaterThan(0);
  }
}
