"""
Airflow DAG for orchestrating the PII pipeline.
"""

from __future__ import annotations

from datetime import datetime

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.providers.apache.beam.operators.beam import BeamRunPythonOperator
from airflow.providers.elasticsearch.hooks.elasticsearch import ElasticsearchHook
from airflow.providers.postgres.operators.postgres import PostgresOperator


def ping_elasticsearch() -> bool:
    """
    Pings Elasticsearch to validate the connection.

    Returns:
        True when Elasticsearch responds to ping.
    """
    hook = ElasticsearchHook(elasticsearch_conn_id="elasticsearch_default")
    client = hook.get_conn()
    return bool(client.ping())


with DAG(
    dag_id="pii_guard_pipeline",
    start_date=datetime(2024, 1, 1),
    schedule="@hourly",
    catchup=False,
    tags=["pii", "beam", "presidio"],
) as dag:
    beam_task = BeamRunPythonOperator(
        task_id="run_beam_pipeline",
        py_file="/opt/airflow/beam/beam_trigger.py",
        pipeline_options={
            "config_path": "/opt/airflow/config/config.yaml",
        },
        py_interpreter="python3",
    )

    postgres_task = PostgresOperator(
        task_id="validate_postgres",
        postgres_conn_id="postgres_default",
        sql="SELECT 1;",
    )

    elastic_task = PythonOperator(
        task_id="ping_elasticsearch",
        python_callable=ping_elasticsearch,
    )

    beam_task >> postgres_task >> elastic_task
