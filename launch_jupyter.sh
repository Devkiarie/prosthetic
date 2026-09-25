#!/usr/bin/env bash
# Launch JupyterLab for FYP ML work
# Usage: bash launch_jupyter.sh
cd "$(dirname "$0")"
echo "Starting JupyterLab at http://localhost:8890"
echo "Kernel: FYP sEMG ML (Python 3.11) — TF 2.16.2"
echo "Press Ctrl+C to stop"
ml/venv/bin/jupyter lab \
  --notebook-dir=ml/notebooks \
  --port=8890 \
  --no-browser \
  --NotebookApp.token='' \
  --NotebookApp.password=''
