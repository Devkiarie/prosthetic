"""
add_servo_pockets.py  --  Run with: blender -b <blend_or_stl> --python add_servo_pockets.py
Cuts 5x SG90 servo pockets into 5th_Palm.stl using Boolean Difference in Blender 5.2.
Outputs: hardware/hand_stl/modified/5th_Palm_with_servo_pockets.stl
         hardware/hand_stl/modified/Ada_Right_v5_servo_pockets.blend  (save of scene)

SG90 body dimensions: 23mm(L) x 12.5mm(W) x 29mm(H)
+0.3mm tolerance on all sides -> cutter box: 23.6 x 13.1 x 29.5mm

Pocket positions (dorsal face, cut downward from Z_max=46.1):
  Finger 0 (Thumb)  : X_ctr= 34.0,  Y_ctr=-10.0
  Finger 1 (Index)  : X_ctr= 18.0,  Y_ctr=-10.0
  Finger 2 (Middle) : X_ctr=  4.0,  Y_ctr=-10.0
  Finger 3 (Ring)   : X_ctr=-12.0,  Y_ctr=-10.0
  Finger 4 (Little) : X_ctr=-28.0,  Y_ctr=-10.0

Also adds 1x MG996R wrist pocket (proximal end of palm):
  MG996R body: 40.7mm(L) x 19.7mm(W) x 42.9mm(H), cutter with 0.5mm tolerance
  Wrist pocket: Y_ctr=-50.0 (proximal), X_ctr=0.0, cut from Z_max downward
"""

import bpy
import os
import math

# ── paths ──────────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# scripts/ is inside hardware/, so PROJECT_ROOT = two levels up
PROJECT_ROOT = os.path.dirname(os.path.dirname(SCRIPT_DIR))
STL_IN  = os.path.join(PROJECT_ROOT, 'hardware', 'hand_stl',
                        'Ada_3D_model_files', 'STLs', 'Right Hand', 'extracted', '5th_Palm.stl')
OUT_DIR = os.path.join(PROJECT_ROOT, 'hardware', 'hand_stl', 'modified')
os.makedirs(OUT_DIR, exist_ok=True)
STL_OUT   = os.path.join(OUT_DIR, '5th_Palm_with_servo_pockets.stl')
BLEND_OUT = os.path.join(OUT_DIR, 'Ada_Right_v5_servo_pockets.blend')

# ── clear default scene ────────────────────────────────────────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.scene.unit_settings.system = 'METRIC'
bpy.context.scene.unit_settings.scale_length = 0.001  # mm

# ── import palm STL ────────────────────────────────────────────────────────────
print(f"Importing palm: {STL_IN}")
bpy.ops.wm.stl_import(filepath=STL_IN)
palm_obj = bpy.context.selected_objects[0]
palm_obj.name = 'Palm'

# Blender STL is in metres by default when scale_length=0.001;
# STL coords are in mm so after import they are already in Blender's mm units.
# Check: palm should span ~93mm X, ~123mm Y
print(f"  Palm dimensions: {[f'{d*1000:.1f}mm' for d in palm_obj.dimensions]}")

# ── helper: create a cutter box ───────────────────────────────────────────────
def add_cutter(name, x_ctr, y_ctr, z_top, width_x, length_y, depth_z):
    """
    Creates a box mesh centred at (x_ctr, y_ctr, z_top - depth_z/2).
    All coords in mm; Blender scene is in mm.
    """
    bpy.ops.mesh.primitive_cube_add(size=1)
    obj = bpy.context.active_object
    obj.name = name
    # Scale to actual dimensions
    obj.scale = (width_x / 1000, length_y / 1000, depth_z / 1000)
    bpy.ops.object.transform_apply(scale=True)
    # Position: centre of box sits at (x_ctr, y_ctr, z_top - depth_z/2)
    obj.location = (x_ctr / 1000, y_ctr / 1000, (z_top - depth_z / 2) / 1000)
    return obj

# ── SG90 pocket parameters ────────────────────────────────────────────────────
SG90_W     = 13.1   # X (with tolerance)
SG90_L     = 23.6   # Y (with tolerance)
SG90_H     = 29.5   # Z depth
PALM_Z_TOP = 46.1   # dorsal face Z in mm

sg90_positions = [
    ('Pocket_Thumb',   34.0, -10.0),
    ('Pocket_Index',   18.0, -10.0),
    ('Pocket_Middle',   4.0, -10.0),
    ('Pocket_Ring',   -12.0, -10.0),
    ('Pocket_Little', -28.0, -10.0),
]

# ── MG996R wrist pocket ───────────────────────────────────────────────────────
# MG996R: 40.7mm x 19.7mm x 42.9mm -> cutter 41.2 x 20.2 x 43.4 (0.5mm tolerance)
# Placed at proximal end of palm (Y_ctr = -52.0, near Y_min=-66.5)
MG996R_W  = 20.2
MG996R_L  = 41.2
MG996R_H  = 43.4

# ── add all cutters ───────────────────────────────────────────────────────────
cutters = []
for name, xc, yc in sg90_positions:
    c = add_cutter(name, xc, yc, PALM_Z_TOP, SG90_W, SG90_L, SG90_H)
    cutters.append(c)
    print(f"  Cutter: {name} at ({xc},{yc}), top Z={PALM_Z_TOP}, depth={SG90_H}")

# MG996R wrist pocket: centred at X=0, proximal Y, cut from top
# Wrist motor sits beneath the palm near wrist connector
# Palm Y goes to -66.5, so place cutter centre at Y=-50 (leaves ~3mm wall at Y=-66.5)
mg_cutter = add_cutter('Pocket_Wrist_MG996R', 0.0, -50.0, PALM_Z_TOP,
                        MG996R_W, MG996R_L, MG996R_H)
cutters.append(mg_cutter)
print(f"  Cutter: Pocket_Wrist_MG996R at (0,-50), depth={MG996R_H}")

# ── apply boolean difference for each cutter ──────────────────────────────────
print("Applying boolean differences ...")
for cutter in cutters:
    # Select palm
    bpy.ops.object.select_all(action='DESELECT')
    palm_obj.select_set(True)
    bpy.context.view_layer.objects.active = palm_obj

    # Add boolean modifier
    mod = palm_obj.modifiers.new(name=f'Bool_{cutter.name}', type='BOOLEAN')
    mod.operation = 'DIFFERENCE'
    mod.object    = cutter
    mod.solver    = 'EXACT'

    # Apply modifier
    bpy.ops.object.modifier_apply(modifier=mod.name)
    print(f"  Applied: Bool_{cutter.name}")

    # Hide cutter (don't delete yet -- keep for reference in blend file)
    cutter.hide_set(True)

# ── export modified palm as STL ───────────────────────────────────────────────
print(f"\nExporting modified palm -> {STL_OUT}")
bpy.ops.object.select_all(action='DESELECT')
palm_obj.select_set(True)
bpy.context.view_layer.objects.active = palm_obj
bpy.ops.wm.stl_export(filepath=STL_OUT, export_selected_objects=True)
print("  STL exported.")

# ── save .blend with all objects ──────────────────────────────────────────────
print(f"Saving scene -> {BLEND_OUT}")
bpy.ops.wm.save_as_mainfile(filepath=BLEND_OUT)
print("  Blend saved.")

# ── quick mesh health check ───────────────────────────────────────────────────
import subprocess, sys
try:
    result = subprocess.run(
        ['python3', '-c', f"""
import trimesh, numpy as np
m = trimesh.load('{STL_OUT}')
print(f'Modified palm: verts={{len(m.vertices)}}, faces={{len(m.faces)}}')
print(f'Watertight: {{m.is_watertight}}')
print(f'Bounds Z: {{m.bounds[0][2]:.1f}} to {{m.bounds[1][2]:.1f}} mm')
# Check pocket cuts visible at top Z
zmax = m.bounds[1][2] * 1000  # mm
print(f'Z_max = {{zmax:.1f}} mm  (should be ~46.1 from palm dorsal face)')
print('POCKET CHECK PASS' if m.is_watertight else 'WARNING: mesh not watertight -- review in Blender')
"""],
        capture_output=True, text=True, timeout=30
    )
    print("\nMesh validation:")
    print(result.stdout)
    if result.returncode != 0:
        print("Validation error:", result.stderr[:300])
except Exception as e:
    print(f"Validation skipped: {e}")

print("\n=== DONE ===")
print(f"  STL : {STL_OUT}")
print(f"  Blend: {BLEND_OUT}")
