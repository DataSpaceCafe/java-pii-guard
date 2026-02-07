package com.example.piiguard.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Root configuration object loaded from config/config.yaml.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {
  @JsonProperty("app")
  private AppSettings app;

  @JsonProperty("beam")
  private BeamSettings beam;

  @JsonProperty("input")
  private InputSettings input;

  @JsonProperty("s3")
  private S3Settings s3;

  @JsonProperty("presidio")
  private PresidioSettings presidio;

  @JsonProperty("postgres")
  private PostgresSettings postgres;

  @JsonProperty("elasticsearch")
  private ElasticsearchSettings elasticsearch;

  @JsonProperty("monitoring")
  private MonitoringSettings monitoring;

  /** Returns application settings. */
  public AppSettings getApp() {
    return app;
  }

  /** Sets application settings. */
  public void setApp(AppSettings app) {
    this.app = app;
  }

  /** Returns Beam settings. */
  public BeamSettings getBeam() {
    return beam;
  }

  /** Sets Beam settings. */
  public void setBeam(BeamSettings beam) {
    this.beam = beam;
  }

  /** Returns input settings. */
  public InputSettings getInput() {
    return input;
  }

  /** Sets input settings. */
  public void setInput(InputSettings input) {
    this.input = input;
  }

  /** Returns S3 settings. */
  public S3Settings getS3() {
    return s3;
  }

  /** Sets S3 settings. */
  public void setS3(S3Settings s3) {
    this.s3 = s3;
  }

  /** Returns Presidio settings. */
  public PresidioSettings getPresidio() {
    return presidio;
  }

  /** Sets Presidio settings. */
  public void setPresidio(PresidioSettings presidio) {
    this.presidio = presidio;
  }

  /** Returns Postgres settings. */
  public PostgresSettings getPostgres() {
    return postgres;
  }

  /** Sets Postgres settings. */
  public void setPostgres(PostgresSettings postgres) {
    this.postgres = postgres;
  }

  /** Returns Elasticsearch settings. */
  public ElasticsearchSettings getElasticsearch() {
    return elasticsearch;
  }

  /** Sets Elasticsearch settings. */
  public void setElasticsearch(ElasticsearchSettings elasticsearch) {
    this.elasticsearch = elasticsearch;
  }

  /** Returns monitoring settings. */
  public MonitoringSettings getMonitoring() {
    return monitoring;
  }

  /** Sets monitoring settings. */
  public void setMonitoring(MonitoringSettings monitoring) {
    this.monitoring = monitoring;
  }

  /** Application-level settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AppSettings {
    private String name;
    private String environment;
    private String logLevel;

    /** Returns app name. */
    public String getName() {
      return name;
    }

    /** Sets app name. */
    public void setName(String name) {
      this.name = name;
    }

    /** Returns environment. */
    public String getEnvironment() {
      return environment;
    }

    /** Sets environment. */
    public void setEnvironment(String environment) {
      this.environment = environment;
    }

    /** Returns log level. */
    public String getLogLevel() {
      return logLevel;
    }

    /** Sets log level. */
    public void setLogLevel(String logLevel) {
      this.logLevel = logLevel;
    }
  }

  /** Beam pipeline settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BeamSettings {
    private String runner;
    private String tempLocation;
    private String stagingLocation;
    private String jobName;

    /** Returns runner name. */
    public String getRunner() {
      return runner;
    }

    /** Sets runner name. */
    public void setRunner(String runner) {
      this.runner = runner;
    }

    /** Returns temp location. */
    public String getTempLocation() {
      return tempLocation;
    }

    /** Sets temp location. */
    public void setTempLocation(String tempLocation) {
      this.tempLocation = tempLocation;
    }

    /** Returns staging location. */
    public String getStagingLocation() {
      return stagingLocation;
    }

    /** Sets staging location. */
    public void setStagingLocation(String stagingLocation) {
      this.stagingLocation = stagingLocation;
    }

    /** Returns job name. */
    public String getJobName() {
      return jobName;
    }

    /** Sets job name. */
    public void setJobName(String jobName) {
      this.jobName = jobName;
    }
  }

  /** Input file settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class InputSettings {
    private String format;
    private String filePattern;
    private String textField;
    private String idField;
    private Integer skipHeaderLines;
    private String csvDelimiter;
    private List<String> csvHeaders;
    private String jsonField;

    /** Returns input format. */
    public String getFormat() {
      return format;
    }

    /** Sets input format. */
    public void setFormat(String format) {
      this.format = format;
    }

    /** Returns file pattern. */
    public String getFilePattern() {
      return filePattern;
    }

    /** Sets file pattern. */
    public void setFilePattern(String filePattern) {
      this.filePattern = filePattern;
    }

    /** Returns text field name. */
    public String getTextField() {
      return textField;
    }

    /** Sets text field name. */
    public void setTextField(String textField) {
      this.textField = textField;
    }

    /** Returns id field name. */
    public String getIdField() {
      return idField;
    }

    /** Sets id field name. */
    public void setIdField(String idField) {
      this.idField = idField;
    }

    /** Returns number of header lines to skip. */
    public Integer getSkipHeaderLines() {
      return skipHeaderLines;
    }

    /** Sets number of header lines to skip. */
    public void setSkipHeaderLines(Integer skipHeaderLines) {
      this.skipHeaderLines = skipHeaderLines;
    }

    /** Returns CSV delimiter. */
    public String getCsvDelimiter() {
      return csvDelimiter;
    }

    /** Sets CSV delimiter. */
    public void setCsvDelimiter(String csvDelimiter) {
      this.csvDelimiter = csvDelimiter;
    }

    /** Returns CSV headers. */
    public List<String> getCsvHeaders() {
      return csvHeaders;
    }

    /** Sets CSV headers. */
    public void setCsvHeaders(List<String> csvHeaders) {
      this.csvHeaders = csvHeaders;
    }

    /** Returns JSON field. */
    public String getJsonField() {
      return jsonField;
    }

    /** Sets JSON field. */
    public void setJsonField(String jsonField) {
      this.jsonField = jsonField;
    }
  }

  /** S3-compatible storage settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class S3Settings {
    private String endpoint;
    private String region;
    private boolean pathStyleAccess;
    private String accessKeyEnv;
    private String secretKeyEnv;

    /** Returns S3 endpoint. */
    public String getEndpoint() {
      return endpoint;
    }

    /** Sets S3 endpoint. */
    public void setEndpoint(String endpoint) {
      this.endpoint = endpoint;
    }

    /** Returns S3 region. */
    public String getRegion() {
      return region;
    }

    /** Sets S3 region. */
    public void setRegion(String region) {
      this.region = region;
    }

    /** Returns path style access flag. */
    public boolean isPathStyleAccess() {
      return pathStyleAccess;
    }

    /** Sets path style access flag. */
    public void setPathStyleAccess(boolean pathStyleAccess) {
      this.pathStyleAccess = pathStyleAccess;
    }

    /** Returns access key env var name. */
    public String getAccessKeyEnv() {
      return accessKeyEnv;
    }

    /** Sets access key env var name. */
    public void setAccessKeyEnv(String accessKeyEnv) {
      this.accessKeyEnv = accessKeyEnv;
    }

    /** Returns secret key env var name. */
    public String getSecretKeyEnv() {
      return secretKeyEnv;
    }

    /** Sets secret key env var name. */
    public void setSecretKeyEnv(String secretKeyEnv) {
      this.secretKeyEnv = secretKeyEnv;
    }
  }

  /** Presidio analyzer/anonymizer settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PresidioSettings {
    private String analyzerUrl;
    private String anonymizerUrl;
    private String language;
    private double scoreThreshold;
    private List<String> entities;
    private String hashSaltEnv;
    private String hashSalt;
    private List<RegexRecognizer> customRecognizers;
    private Map<String, Map<String, Object>> anonymizers;

    /** Returns analyzer URL. */
    public String getAnalyzerUrl() {
      return analyzerUrl;
    }

    /** Sets analyzer URL. */
    public void setAnalyzerUrl(String analyzerUrl) {
      this.analyzerUrl = analyzerUrl;
    }

    /** Returns anonymizer URL. */
    public String getAnonymizerUrl() {
      return anonymizerUrl;
    }

    /** Sets anonymizer URL. */
    public void setAnonymizerUrl(String anonymizerUrl) {
      this.anonymizerUrl = anonymizerUrl;
    }

    /** Returns language code. */
    public String getLanguage() {
      return language;
    }

    /** Sets language code. */
    public void setLanguage(String language) {
      this.language = language;
    }

    /** Returns score threshold. */
    public double getScoreThreshold() {
      return scoreThreshold;
    }

    /** Sets score threshold. */
    public void setScoreThreshold(double scoreThreshold) {
      this.scoreThreshold = scoreThreshold;
    }

    /** Returns entity types. */
    public List<String> getEntities() {
      return entities;
    }

    /** Sets entity types. */
    public void setEntities(List<String> entities) {
      this.entities = entities;
    }

    /** Returns hash salt env var name. */
    public String getHashSaltEnv() {
      return hashSaltEnv;
    }

    /** Sets hash salt env var name. */
    public void setHashSaltEnv(String hashSaltEnv) {
      this.hashSaltEnv = hashSaltEnv;
    }

    /** Returns hash salt default value. */
    public String getHashSalt() {
      return hashSalt;
    }

    /** Sets hash salt default value. */
    public void setHashSalt(String hashSalt) {
      this.hashSalt = hashSalt;
    }

    /** Returns custom recognizers. */
    public List<RegexRecognizer> getCustomRecognizers() {
      return customRecognizers;
    }

    /** Sets custom recognizers. */
    public void setCustomRecognizers(List<RegexRecognizer> customRecognizers) {
      this.customRecognizers = customRecognizers;
    }

    /** Returns anonymizer operators. */
    public Map<String, Map<String, Object>> getAnonymizers() {
      return anonymizers;
    }

    /** Sets anonymizer operators. */
    public void setAnonymizers(Map<String, Map<String, Object>> anonymizers) {
      this.anonymizers = anonymizers;
    }
  }

  /** Regex recognizer definition. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class RegexRecognizer {
    private String name;
    private String supportedEntity;
    private String regex;
    private String flags;
    private double score;

    /** Returns recognizer name. */
    public String getName() {
      return name;
    }

    /** Sets recognizer name. */
    public void setName(String name) {
      this.name = name;
    }

    /** Returns supported entity. */
    public String getSupportedEntity() {
      return supportedEntity;
    }

    /** Sets supported entity. */
    public void setSupportedEntity(String supportedEntity) {
      this.supportedEntity = supportedEntity;
    }

    /** Returns regex pattern. */
    public String getRegex() {
      return regex;
    }

    /** Sets regex pattern. */
    public void setRegex(String regex) {
      this.regex = regex;
    }

    /** Returns regex flags. */
    public String getFlags() {
      return flags;
    }

    /** Sets regex flags. */
    public void setFlags(String flags) {
      this.flags = flags;
    }

    /** Returns recognizer score. */
    public double getScore() {
      return score;
    }

    /** Sets recognizer score. */
    public void setScore(double score) {
      this.score = score;
    }
  }

  /** Postgres JDBC settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PostgresSettings {
    private String jdbcUrl;
    private String userEnv;
    private String passwordEnv;
    private String table;
    private int batchSize;

    /** Returns JDBC URL. */
    public String getJdbcUrl() {
      return jdbcUrl;
    }

    /** Sets JDBC URL. */
    public void setJdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
    }

    /** Returns user env var name. */
    public String getUserEnv() {
      return userEnv;
    }

    /** Sets user env var name. */
    public void setUserEnv(String userEnv) {
      this.userEnv = userEnv;
    }

    /** Returns password env var name. */
    public String getPasswordEnv() {
      return passwordEnv;
    }

    /** Sets password env var name. */
    public void setPasswordEnv(String passwordEnv) {
      this.passwordEnv = passwordEnv;
    }

    /** Returns table name. */
    public String getTable() {
      return table;
    }

    /** Sets table name. */
    public void setTable(String table) {
      this.table = table;
    }

    /** Returns batch size. */
    public int getBatchSize() {
      return batchSize;
    }

    /** Sets batch size. */
    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }
  }

  /** Elasticsearch settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ElasticsearchSettings {
    private String url;
    private String usernameEnv;
    private String passwordEnv;
    private String logsIndexPrefix;
    private String metricsIndexPrefix;
    private int bulkSize;
    private int bulkFlushSeconds;

    /** Returns Elasticsearch URL. */
    public String getUrl() {
      return url;
    }

    /** Sets Elasticsearch URL. */
    public void setUrl(String url) {
      this.url = url;
    }

    /** Returns username env var name. */
    public String getUsernameEnv() {
      return usernameEnv;
    }

    /** Sets username env var name. */
    public void setUsernameEnv(String usernameEnv) {
      this.usernameEnv = usernameEnv;
    }

    /** Returns password env var name. */
    public String getPasswordEnv() {
      return passwordEnv;
    }

    /** Sets password env var name. */
    public void setPasswordEnv(String passwordEnv) {
      this.passwordEnv = passwordEnv;
    }

    /** Returns logs index prefix. */
    public String getLogsIndexPrefix() {
      return logsIndexPrefix;
    }

    /** Sets logs index prefix. */
    public void setLogsIndexPrefix(String logsIndexPrefix) {
      this.logsIndexPrefix = logsIndexPrefix;
    }

    /** Returns metrics index prefix. */
    public String getMetricsIndexPrefix() {
      return metricsIndexPrefix;
    }

    /** Sets metrics index prefix. */
    public void setMetricsIndexPrefix(String metricsIndexPrefix) {
      this.metricsIndexPrefix = metricsIndexPrefix;
    }

    /** Returns bulk size. */
    public int getBulkSize() {
      return bulkSize;
    }

    /** Sets bulk size. */
    public void setBulkSize(int bulkSize) {
      this.bulkSize = bulkSize;
    }

    /** Returns bulk flush seconds. */
    public int getBulkFlushSeconds() {
      return bulkFlushSeconds;
    }

    /** Sets bulk flush seconds. */
    public void setBulkFlushSeconds(int bulkFlushSeconds) {
      this.bulkFlushSeconds = bulkFlushSeconds;
    }
  }

  /** Monitoring settings. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MonitoringSettings {
    private boolean logsEnabled;
    private boolean metricsEnabled;

    /** Returns logs enabled flag. */
    public boolean isLogsEnabled() {
      return logsEnabled;
    }

    /** Sets logs enabled flag. */
    public void setLogsEnabled(boolean logsEnabled) {
      this.logsEnabled = logsEnabled;
    }

    /** Returns metrics enabled flag. */
    public boolean isMetricsEnabled() {
      return metricsEnabled;
    }

    /** Sets metrics enabled flag. */
    public void setMetricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
    }
  }
}
