# Ada Hand v1.1 — 3D Model Files

Source: [Open Bionics Ada Hand v1.1](https://github.com/Open-Bionics/Ada_3D_model_files)
License: CC BY-SA 4.0

## What's here (not committed — too large for GitHub)

The `STLs/` folder contains 39 STL files for the Ada Hand v1.1:
- `STLs/Right Hand/R_Ada_v1.1_Palm.stl` — main palm body
- `STLs/Right Hand/extracted/0th_Thumb.stl` through `4th_finger.stl` — individual finger assemblies
- `STLs/Right Hand/extracted/5th_Palm.stl` — palm variant used for servo pockets

## Modified files (tracked separately in `../modified/`)

| File | What | Status |
|---|---|---|
| `../modified/5th_Palm_with_servo_pockets.stl` | Palm with 5× SG90 + 1× MG996R pockets cut | Print-ready |
| `../modified/Full_Hand_with_servo_pockets.blend` | Blender source for full assembly | Active design file |

## How to get the STL files

```bash
# Option 1: from the backup zip (if you have it)
unzip backup_20261005_235508.zip "*/STLs/*" -d restore/

# Option 2: clone directly from Open Bionics
git clone https://github.com/Open-Bionics/Ada_3D_model_files.git Ada_3D_model_files
```

## Servo pocket dimensions (cut into 5th_Palm.stl)

| Servo | Pocket size | Qty |
|---|---|---|
| SG90 (fingers) | 23 × 12.5 × 29 mm | 5 |
| MG996R (wrist) | 40 × 20 × 38 mm | 1 |
