package com.example.piiguard.storage;

import com.example.piiguard.processing.PiiRecord;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.beam.sdk.io.jdbc.JdbcIO;

/**
 * Builds JDBC write transforms for Postgres.
 */
public final class PostgresWriter {
  private PostgresWriter() {
  }

  /**
   * Creates a JdbcIO write transform for PiiRecord.
   *
   * @param jdbcUrl JDBC URL
   * @param username DB username
   * @param password DB password
   * @param table target table
   * @param batchSize batch size
   * @return JdbcIO write transform
   */
  public static JdbcIO.Write<PiiRecord> build(String jdbcUrl, String username, String password,
                                              String table, int batchSize) {
    var statement = String.format(
        "INSERT INTO %s (record_id, source, original_text, anonymized_text, pii_count, entity_types, entities_json, processed_at, success, error_message) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)", table);

    return JdbcIO.<PiiRecord>write()
        .withDataSourceConfiguration(
            JdbcIO.DataSourceConfiguration.create("org.postgresql.Driver", jdbcUrl)
                .withUsername(username)
                .withPassword(password))
        .withStatement(statement)
        .withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<>() {
          /**
           * Sets prepared statement parameters for a PiiRecord.
           *
           * @param record record to persist
           * @param statement JDBC statement
           * @throws SQLException when setting parameters fails
           */
          @Override
          public void setParameters(PiiRecord record, PreparedStatement statement) throws SQLException {
            statement.setString(1, record.recordId);
            statement.setString(2, record.source);
            statement.setString(3, record.originalText);
            statement.setString(4, record.anonymizedText);
            statement.setInt(5, record.piiCount);
            statement.setString(6, String.join(",", record.entityTypes));
            statement.setString(7, record.entitiesJson);
            statement.setObject(8, record.processedAt);
            statement.setBoolean(9, record.success);
            statement.setString(10, record.errorMessage);
          }
        })
        .withBatchSize(batchSize);
  }
}
