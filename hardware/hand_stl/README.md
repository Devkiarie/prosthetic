# Prosthetic Hand STL Files

## Ada Hand v1.1 (Open Bionics)
Source: https://github.com/Open-Bionics/Ada_3D_model_files
License: Creative Commons Attribution-ShareAlike 4.0

Contains:
- STLs/Right Hand/R_Ada_v1.1_Palm.stl  (174×193×42mm — fits 5× SG90 dorsal)
- STLs/Right Hand/R_Ada_v1.1_Back_Cover.stl
- STLs/Right Hand/R_Ada_v1.1_PCB_Tray_Lower.stl
- STLs/Right Hand/R_Ada_v1.1_PCB_Tray_Upper.stl
- Blender Files/Right Hand/Ada Right v1.1.blend (editable source)

## Modifications Required for FYP
1. Open Ada Right v1.1.blend in Blender
2. Add 5 SG90 servo pockets (23×12.5×29mm each) to dorsal palm cavity
3. Add tendon channel (2mm dia) through each finger
4. Add MG996R pocket at wrist base
5. Export modified palm as STL
6. Slice in Cura (0.2mm, PETG, 30% infill)
