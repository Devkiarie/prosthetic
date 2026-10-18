"""
draw_schematic.py — Generate sEMG AFE circuit diagram using schemdraw
Ian Kiarie | FYP ENE212-0069/2022 | JKUAT ECE

Generates a properly connected circuit diagram showing all stages:
  Electrode → INA128 G=51 → Sallen-Key HPF 20Hz → Twin-T Notch 50Hz → Variable Gain → ESP32-S3 ADC1

Output:
    hardware/kicad/semg_ch1_schematic.png  (200 DPI)
    hardware/kicad/semg_ch1_schematic.svg  (vector — for thesis)

Run:
    cd "~/school/Final Year project"
    source ml/venv/bin/activate
    python3 ml/scripts/draw_schematic.py
"""
import schemdraw, schemdraw.elements as elm
import matplotlib; matplotlib.use('Agg')
from pathlib import Path

BASE = Path(__file__).parent.parent.parent
OUT  = BASE / 'hardware' / 'kicad'

BG='#0f172a'; FG='#e2e8f0'; CYAN='#22d3ee'; EMERALD='#34d399'
AMBER='#fbbf24'; VIOLET='#a78bfa'; ROSE='#fb7185'

def draw_channel(ch: int = 1) -> None:
    with schemdraw.Drawing(show=False) as d:
        d.config(fontsize=9, color=FG, bgcolor=BG, lw=1.5, unit=3.0)

        # Electrode + protection
        elm.Dot(open=True).color(ROSE).label('IN+', 'left')
        elm.Resistor().right().color(ROSE).label('10kΩ', 'top')

        # INA128
        ina = elm.Opamp().anchor('in1').right().color(CYAN)
        elm.Label().at(ina.center).label(f'INA128\nG=51 (U{ch})', 'center').color(CYAN)
        d.push()
        elm.Line().at(ina.in2).left().length(1.5).color(ROSE)
        elm.Resistor().left().color(ROSE).label('10kΩ', 'bot')
        elm.Dot(open=True).color(ROSE).label('IN−', 'left')
        d.pop()
        d.push()
        elm.Resistor().at(ina.in1).toy(ina.in2).color(CYAN).label('R_G\n1kΩ 1%', 'right')
        d.pop()

        # HPF
        elm.Line().at(ina.out).right().length(0.5).color(EMERALD)
        elm.Capacitor().right().color(EMERALD).label('470nF film', 'top')
        elm.Resistor().right().color(EMERALD).label('12kΩ', 'top')
        j_hpf = d.here
        d.push()
        elm.Capacitor().at(j_hpf).down().color(EMERALD).label('470nF film', 'right')
        elm.Ground().color(EMERALD)
        d.pop()
        elm.Resistor().right().color(EMERALD).label('24kΩ', 'top')
        hpf = elm.Opamp().anchor('in1').right().color(EMERALD)
        elm.Label().at(hpf.center).label('HPF 20Hz', 'center').color(EMERALD)
        d.push()
        elm.Line().at(hpf.out).up().length(1.5).color(EMERALD)
        elm.Line().left().tox(hpf.in2).color(EMERALD)
        elm.Line().down().toy(hpf.in2).color(EMERALD)
        d.pop()

        # Notch
        elm.Line().at(hpf.out).right().length(0.5).color(AMBER)
        elm.Resistor().right().color(AMBER).label('10kΩ 1%', 'top')
        j_notch = d.here
        d.push()
        elm.Capacitor().at(j_notch).down().color(AMBER).label('680nF 1%', 'right')
        elm.Ground().color(AMBER)
        d.pop()
        elm.Resistor().right().color(AMBER).label('10kΩ 1%', 'top')

        # Variable gain
        elm.Line().right().length(0.5).color(VIOLET)
        vg = elm.Opamp().anchor('in1').right().color(VIOLET)
        elm.Label().at(vg.center).label('Var Gain\n1-11x', 'center').color(VIOLET)
        d.push()
        elm.Line().at(vg.out).up().length(1.5).color(VIOLET)
        elm.ResistorVar().left().tox(vg.in2).color(VIOLET).label('100kΩ B100K', 'top')
        elm.Line().down().toy(vg.in2).color(VIOLET)
        d.pop()

        # ADC input
        elm.Resistor().at(vg.out).right().color(CYAN).label('1kΩ', 'top')
        j_adc = d.here
        d.push()
        elm.Capacitor().at(j_adc).down().color(CYAN).label('33nF', 'right')
        elm.Ground().color(CYAN)
        d.pop()
        elm.Line().right().length(0.5).color(CYAN)
        elm.Dot(open=True).color(CYAN).label(f'GPIO{ch}\n(ADC1_CH{ch-1})\nESP32-S3', 'right')

        d.save(str(OUT / f'semg_ch{ch}_schematic.png'), dpi=200)
        d.save(str(OUT / f'semg_ch{ch}_schematic.svg'))
        print(f"Saved: semg_ch{ch}_schematic.png + .svg")

if __name__ == '__main__':
    print("Generating sEMG AFE schematic (CH1)...")
    draw_channel(1)
    print("\nFor CH2-CH4: same circuit, change GPIO pin (GPIO2/3/4) and U number.")
    print("The 1.65V REF buffer (MCP6002 unity follower) is shared across all 4 channels.")
