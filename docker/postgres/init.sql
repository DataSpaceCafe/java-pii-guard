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
