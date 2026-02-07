# java-pii-guard

PII Detection → Anonymization → Storage pipeline using Apache Beam (Java 21) + Microsoft Presidio services.

## Overview
- **Ingestion**: Apache Beam reads CSV/JSON from S3-compatible storage (MinIO / GCS S3).
- **Detection**: Presidio Analyzer (model + custom regex) via REST.
- **Anonymization**: Presidio Anonymizer (hash + redaction, salt = `PLACEHOLDER_SALT`).
- **Storage**: Beam JDBC sink → PostgreSQL (batch insert).
- **Monitoring**: Logs + metrics bulk-indexed into Elasticsearch (`ppi-logs-*`, `ppi-metrics-*`).

## Quick Start (Docker Compose)
1. Start services:
   ```bash
   docker compose up -d
   ```
2. The pipeline container will start automatically and process seed data from MinIO.
3. Open Kibana at `http://localhost:5601` and create data views for `ppi-logs-*` and `ppi-metrics-*`.

Seed CSV data lives in `data/seed/input.csv` and is uploaded into MinIO by the `minio-init` service.

## Local Run (Maven)
```bash
mvn -B -DskipTests package
java -jar target/java-pii-guard.jar --config config/config.yaml
```

## Configuration
`config/config.yaml` controls Beam, S3, Presidio, Postgres, and Elasticsearch settings. Secrets are **never** hardcoded; provide them via environment variables:

- `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
- `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `ELASTIC_USERNAME`, `ELASTIC_PASSWORD`
- `PII_HASH_SALT`
- `CONFIG_PATH` (optional override)
- `LOG_LEVEL`

## Cloud Run / Cloud Deployment
- **Container**: build the image and deploy to Cloud Run (or any Kubernetes runtime).
- **Beam Runner**: for production, switch `beam.runner` to `DataflowRunner` and provide GCS temp/staging paths.
- **Storage**: update `input.filePattern` and `s3.endpoint` to your object store.

Example build:
```bash
docker build -t java-pii-guard:latest .
```

## Monitoring & Dashboard
- Logs index: `ppi-logs-*`
- Metrics index: `ppi-metrics-*`
- Dashboard & alert sketch (PII count threshold): see `docs/kibana.md`

## Database Schema
Postgres table is created by `docker/postgres/init.sql`:
- `pii_records(record_id, source, original_text, anonymized_text, pii_count, entity_types, entities_json, processed_at, success, error_message)`

## Tests
- **JUnit 5 + Testcontainers**: `src/test/java/...`
- **Python testcontainers** (optional): `airflow/tests/`

Run tests:
```bash
mvn -B verify
pytest airflow/tests
```

## Airflow (Optional)
- DAG: `airflow/dags/pii_pipeline_dag.py`
- Beam trigger stub: `airflow/beam/beam_trigger.py`

The DAG shows how to orchestrate Beam + Postgres validation + Elasticsearch health checks.
