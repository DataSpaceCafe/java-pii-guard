"""
Placeholder Beam trigger script for Airflow orchestration.
"""

from __future__ import annotations

import subprocess
from typing import Sequence


def run_pipeline(args: Sequence[str] | None = None) -> int:
    """
    Runs the Java pipeline using a subprocess call.

    Args:
        args: Optional CLI args for the Java command.

    Returns:
        Exit code from the subprocess.
    """
    command = ["java", "-jar", "/opt/airflow/java-pii-guard.jar", "--config", "/opt/airflow/config/config.yaml"]
    if args:
        command.extend(args)
    result = subprocess.run(command, check=False)
    return int(result.returncode)


if __name__ == "__main__":
    raise SystemExit(run_pipeline())
