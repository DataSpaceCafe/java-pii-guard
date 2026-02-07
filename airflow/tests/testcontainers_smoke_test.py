"""
Pytest testcontainers smoke tests.
"""

from __future__ import annotations

import psycopg2
from testcontainers.postgres import PostgresContainer


def test_postgres_container() -> None:
    """
    Ensures a Postgres container can be started and queried.
    """
    with PostgresContainer("postgres:16-alpine") as postgres:
        connection = psycopg2.connect(postgres.get_connection_url())
        try:
            with connection.cursor() as cursor:
                cursor.execute("SELECT 1;")
                result = cursor.fetchone()
            assert result is not None
            assert result[0] == 1
        finally:
            connection.close()
