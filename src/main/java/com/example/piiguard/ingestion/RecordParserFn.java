package com.example.piiguard.ingestion;

import com.example.piiguard.config.AppConfig;
import com.example.piiguard.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses CSV or JSON lines into InputRecord objects.
 */
public class RecordParserFn extends DoFn<String, InputRecord> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordParserFn.class);

  private final AppConfig.InputSettings inputSettings;

  /**
   * Creates a parser with input settings.
   *
   * @param inputSettings input settings
   */
  public RecordParserFn(AppConfig.InputSettings inputSettings) {
    this.inputSettings = inputSettings;
  }

  /**
   * Parses a line into InputRecord and outputs it.
   *
   * @param context Beam process context
   */
  @ProcessElement
  public void processElement(ProcessContext context) {
    var line = context.element();
    if (line == null || line.isBlank()) {
      return;
    }

    try {
      var format = inputSettings.getFormat();
      if (format != null && format.equalsIgnoreCase("json")) {
        parseJson(line, context);
      } else {
        parseCsv(line, context);
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to parse line", e);
    }
  }

  /**
   * Parses a JSON line into an InputRecord.
   *
   * @param line JSON line
   * @param context Beam context
   * @throws Exception when parsing fails
   */
  private void parseJson(String line, ProcessContext context) throws Exception {
    var mapper = JsonUtils.mapper();
    var attributes = mapper.readValue(line, new TypeReference<Map<String, Object>>() {
    });
    var record = buildRecord(attributes);
    if (record != null) {
      context.output(record);
    }
  }

  /**
   * Parses a CSV line into an InputRecord.
   *
   * @param line CSV line
   * @param context Beam context
   * @throws Exception when parsing fails
   */
  private void parseCsv(String line, ProcessContext context) throws Exception {
    var headers = inputSettings.getCsvHeaders();
    var formatBuilder = CSVFormat.DEFAULT.builder();
    if (headers != null && !headers.isEmpty()) {
      formatBuilder.setHeader(headers.toArray(String[]::new));
      formatBuilder.setSkipHeaderRecord(false);
    }
    if (inputSettings.getCsvDelimiter() != null && !inputSettings.getCsvDelimiter().isBlank()) {
      formatBuilder.setDelimiter(inputSettings.getCsvDelimiter());
    }
    try (var parser = CSVParser.parse(line, formatBuilder.build())) {
      var attributes = new HashMap<String, Object>();
      var records = parser.getRecords();
      if (records.isEmpty()) {
        return;
      }
      var csvRecord = records.get(0);
      if (headers != null && !headers.isEmpty()) {
        for (var header : headers) {
          attributes.put(header, csvRecord.get(header));
        }
      } else {
        var values = new HashMap<String, Object>();
        for (var i = 0; i < csvRecord.size(); i++) {
          values.put("col" + i, csvRecord.get(i));
        }
        attributes.putAll(values);
      }
      var record = buildRecord(attributes);
      if (record != null) {
        context.output(record);
      }
    }
  }

  /**
   * Builds an InputRecord from parsed attributes.
   *
   * @param attributes parsed attributes
   * @return input record or null when missing required fields
   */
  private InputRecord buildRecord(Map<String, Object> attributes) {
    var textField = inputSettings.getTextField();
    var idField = inputSettings.getIdField();
    var textValue = attributeAsString(attributes, textField);
    if (textValue == null || textValue.isBlank()) {
      LOGGER.debug("Skipping record without text field: {}", textField);
      return null;
    }
    var recordId = attributeAsString(attributes, idField);
    if (recordId == null || recordId.isBlank()) {
      recordId = UUID.randomUUID().toString();
    }
    var source = attributeAsString(attributes, "source");
    if (source == null || source.isBlank()) {
      source = "s3";
    }
    return new InputRecord(recordId, source, textValue, attributes, Instant.now());
  }

  /**
   * Reads an attribute as a string.
   *
   * @param attributes attributes map
   * @param field field name
   * @return string value or null
   */
  private String attributeAsString(Map<String, Object> attributes, String field) {
    if (field == null || field.isBlank()) {
      return null;
    }
    var value = attributes.get(field);
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }
}
