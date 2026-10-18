"""
copy_model.py — PlatformIO pre-build script
Copies semg_v3_weights.h from ml/models/ to firmware/model/
Run automatically before each build: `extra_scripts = pre:tools/copy_model.py`
"""
import shutil
from pathlib import Path
Import("env")  # noqa: F821 — PlatformIO injects this

project_dir = Path(env["PROJECT_DIR"])
src = project_dir.parent / "ml" / "models" / "semg_v3_weights.h"
dst = project_dir / "model" / "semg_v3_weights.h"
dst.parent.mkdir(parents=True, exist_ok=True)

if src.exists():
    shutil.copy2(str(src), str(dst))
    print(f"[pre-build] Copied model: {src} → {dst}")
else:
    # Create a placeholder so the build doesn't fail
    # Replace with real model by running: python3 ml/scripts/train_v3.py
    dst.write_text("""
// PLACEHOLDER — Run train_v3.py to generate real model weights
#pragma once
#include <stdint.h>
// Placeholder 1-byte model (will crash at inference — replace before use)
const unsigned int semg_model_len = 1;
alignas(8) const uint8_t semg_model_data[] = {0x00};
""")
    print(f"[pre-build] WARNING: model not found at {src}. Placeholder created.")
    print("[pre-build] Run: cd 'ml' && python3 scripts/train_v3.py")
