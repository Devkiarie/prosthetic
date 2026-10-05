"""
assemble_hand_with_pockets.py
Imports all Ada Right Hand STLs (palm + 5 fingers) into a single Blender scene,
cuts SG90 servo pockets into the palm, and saves both STL and .blend.

Run with:
  blender -b --python hardware/scripts/assemble_hand_with_pockets.py

Output:
  hardware/hand_stl/modified/Full_Hand_with_servo_pockets.blend
  hardware/hand_stl/modified/5th_Palm_with_servo_pockets.stl   (palm only, printable)
"""

import bpy, os, math

# ── paths ──────────────────────────────────────────────────────────────────────
SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(os.path.dirname(SCRIPT_DIR))
STL_DIR = os.path.join(PROJECT_ROOT, 'hardware', 'hand_stl',
                       'Ada_3D_model_files', 'STLs', 'Right Hand', 'extracted')
OUT_DIR = os.path.join(PROJECT_ROOT, 'hardware', 'hand_stl', 'modified')
os.makedirs(OUT_DIR, exist_ok=True)

BLEND_OUT = os.path.join(OUT_DIR, 'Full_Hand_with_servo_pockets.blend')
PALM_OUT  = os.path.join(OUT_DIR, '5th_Palm_with_servo_pockets.stl')

# ── clear default scene ────────────────────────────────────────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.scene.unit_settings.system = 'METRIC'
# Leave scale_length = 1.0 (default). Blender STL importer brings coords in as
# metres numerically but STL files store mm, so everything is already 1 unit = 1mm.
# Do NOT set scale_length=0.001 -- that causes a 1000x inflation.

# ── import all parts ───────────────────────────────────────────────────────────
PARTS = {
    'Palm'    : '5th_Palm.stl',
    'Thumb'   : '0th_Thumb.stl',
    'Index'   : '1st_finger.stl',
    'Middle'  : '2nd_finger.stl',
    'Ring'    : '3rd_finger.stl',
    'Little'  : '4th_finger.stl',
}

imported = {}
for label, fname in PARTS.items():
    path = os.path.join(STL_DIR, fname)
    if not os.path.exists(path):
        print(f"  WARNING: {path} not found, skipping")
        continue
    bpy.ops.wm.stl_import(filepath=path)
    obj = bpy.context.selected_objects[0]
    obj.name = label
    dims = [d * 1000 for d in obj.dimensions]   # convert to mm for display
    print(f"  {label}: {dims[0]:.1f} x {dims[1]:.1f} x {dims[2]:.1f} mm")
    imported[label] = obj

# Assign distinct colours per part (viewport shading only)
COLOURS = {
    'Palm'  : (0.80, 0.70, 0.60, 1.0),   # beige
    'Thumb' : (0.95, 0.40, 0.40, 1.0),   # red
    'Index' : (0.40, 0.80, 0.40, 1.0),   # green
    'Middle': (0.40, 0.60, 0.95, 1.0),   # blue
    'Ring'  : (0.90, 0.80, 0.30, 1.0),   # yellow
    'Little': (0.80, 0.50, 0.90, 1.0),   # purple
}
for label, obj in imported.items():
    mat = bpy.data.materials.new(name=label + '_mat')
    mat.diffuse_color = COLOURS.get(label, (0.7, 0.7, 0.7, 1.0))
    obj.data.materials.append(mat)

# ── SG90 pocket dimensions ─────────────────────────────────────────────────────
# SG90 body: 23mm(L) x 12.5mm(W) x 29mm(H)  +0.3mm tolerance on all sides
SG90_CUT_X = 23.6   # length (along X)
SG90_CUT_Y = 13.1   # width  (along Y)
SG90_CUT_Z = 29.5   # depth (cut down from dorsal face)

# MG996R wrist: 40.7mm(L) x 19.7mm(W) x 42.9mm(H)  +0.5mm tolerance
MG_CUT_X = 41.7
MG_CUT_Y = 20.7
MG_CUT_Z = 43.4

def add_cutter(name, x_ctr, y_ctr, z_top, cx, cy, cz):
    """Adds a box cutter.  z_top is the top face Z (mm units); box extends DOWN by cz."""
    bpy.ops.mesh.primitive_cube_add(size=1)
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = (cx, cy, cz)                       # 1 Blender unit = 1 mm
    obj.location = (x_ctr, y_ctr, z_top - cz / 2)
    bpy.ops.object.transform_apply(scale=True)
    return obj

# Get palm Z max
if 'Palm' in imported:
    palm_obj = imported['Palm']
    # Bounding box corners in world space
    bb = [palm_obj.matrix_world @ v.co for v in palm_obj.data.vertices]
    z_max_world = max(v.z for v in bb)
    z_max_mm    = z_max_world * 1000
    print(f"\n  Palm Z max: {z_max_mm:.2f} mm")

    # 5 x SG90 finger pockets on dorsal face
    FINGER_POCKETS = [
        ('Cut_Thumb',  34.0, -10.0),
        ('Cut_Index',  18.0, -10.0),
        ('Cut_Middle',  4.0, -10.0),
        ('Cut_Ring',  -12.0, -10.0),
        ('Cut_Little',-28.0, -10.0),
    ]
    cutters = []
    for cname, xc, yc in FINGER_POCKETS:
        c = add_cutter(cname, xc, yc, z_max_mm, SG90_CUT_X, SG90_CUT_Y, SG90_CUT_Z)
        c.display_type = 'WIRE'
        cutters.append(c)

    # 1 x MG996R wrist pocket at proximal end
    wrist_cutter = add_cutter('Cut_Wrist', 0.0, -50.0, z_max_mm,
                               MG_CUT_X, MG_CUT_Y, MG_CUT_Z)
    wrist_cutter.display_type = 'WIRE'
    cutters.append(wrist_cutter)

    # Boolean difference: cut all pockets from palm
    for c in cutters:
        mod = palm_obj.modifiers.new(name=f'Bool_{c.name}', type='BOOLEAN')
        mod.operation = 'DIFFERENCE'
        mod.solver = 'EXACT'   # Blender 5.x: FLOAT | EXACT | MANIFOLD (not FAST)
        mod.object = c
        bpy.context.view_layer.objects.active = palm_obj
        bpy.ops.object.modifier_apply(modifier=mod.name)
        bpy.data.objects.remove(c, do_unlink=True)

    print(f"  {len(cutters)} pockets cut into palm.")

    # Export palm-only STL for printing
    bpy.ops.object.select_all(action='DESELECT')
    palm_obj.select_set(True)
    bpy.context.view_layer.objects.active = palm_obj
    bpy.ops.wm.stl_export(filepath=PALM_OUT, export_selected_objects=True)
    print(f"  Palm STL exported -> {PALM_OUT}")

# ── add camera + sun for a nice viewport render ────────────────────────────────
bpy.ops.object.camera_add(location=(150, -250, 200))  # mm units
cam = bpy.context.active_object
cam.rotation_euler = (math.radians(55), 0, math.radians(25))
bpy.context.scene.camera = cam

bpy.ops.object.light_add(type='SUN', location=(0.3, -0.3, 0.5))
sun = bpy.context.active_object
sun.data.energy = 3.0

# ── save .blend ────────────────────────────────────────────────────────────────
bpy.ops.wm.save_as_mainfile(filepath=BLEND_OUT)
print(f"\n=== DONE ===")
print(f"  Full assembly blend: {BLEND_OUT}")
print(f"  Palm STL (printable): {PALM_OUT}")
