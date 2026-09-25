#!/usr/bin/env bash
# run_all_sims.sh — Run all ngspice simulations and print pass/fail summary
# Usage: cd hardware/simulation && bash run_all_sims.sh

set -euo pipefail

SIMDIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

run_sim() {
  local file="$1"
  local name="$2"
  echo ""
  echo "========================================"
  echo "Running: $name"
  echo "========================================"
  output=$(ngspice -b "$file" 2>&1)
  if echo "$output" | grep -qi "error"; then
    echo "FAIL — errors detected:"
    echo "$output" | grep -i error
    FAIL=$((FAIL+1))
  else
    echo "$output" | grep -E "(Lower|Upper|Notch|Passband|Total|After|dB|target|Hz:)" | grep -v "^$"
    PASS=$((PASS+1))
    echo "PASS"
  fi
}

cd "$SIMDIR"

run_sim "01_INA128_gain.cir"          "SIM 01: INA128 Gain"
run_sim "02_sallen_key_bandpass.cir"  "SIM 02: Sallen-Key Bandpass 20-500 Hz"
run_sim "03_twin_t_notch.cir"         "SIM 03: Twin-T Notch 50 Hz"
run_sim "04_full_signal_chain.cir"    "SIM 04: Full Signal Chain"

echo ""
echo "========================================"
echo "SUMMARY: $PASS passed, $FAIL failed"
echo "========================================"
