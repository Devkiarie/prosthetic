"""
draw_schematic.py — Generate sEMG AFE circuit schematic diagram using schemdraw
Ian Kiarie | FYP ENE212-0069/2022 | JKUAT ECE

Produces a properly connected circuit diagram showing all stages:
  Electrodes → INA128 → Sallen-Key BPF → Twin-T Notch → Variable Gain → ESP32-S3

Output: hardware/kicad/semg_afe_diagram.png  (high-res PNG)
        hardware/kicad/semg_afe_diagram.svg   (vector, for thesis)

Run:
    source ml/venv/bin/activate
    python3 ml/scripts/draw_schematic.py
"""
import schemdraw
import schemdraw.elements as elm
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

BASE = Path(__file__).parent.parent.parent
OUT  = BASE / 'hardware' / 'kicad'

# ── Colour palette (matches project theme) ────────────────────────────────────
BG       = '#0f172a'
FG       = '#e2e8f0'
CYAN     = '#22d3ee'
EMERALD  = '#34d399'
AMBER    = '#fbbf24'
VIOLET   = '#a78bfa'
ROSE     = '#fb7185'
ORANGE   = '#fb923c'
MUTED    = '#64748b'

def draw_one_channel(channel_num: int = 1) -> str:
    """Draw the full signal chain for one channel and return the output path."""

    with schemdraw.Drawing(show=False) as d:
        d.config(fontsize=10, color=FG, bgcolor=BG, lw=1.5)

        # ── Electrodes ──────────────────────────────────────────────────────────
        d.push()
        elm.Dot(open=True).color(ROSE).label(f'IN+\n(electrode +)', loc='left')
        R_prot1 = elm.Resistor().right().color(ROSE).label('R_PROT\n10kΩ', loc='top')
        ina_inp = d.here  # save connection point

        d.pop()
        d.push()
        elm.Dot(open=True).at(d.here).color(ROSE).down().length(2)
        elm.Dot(open=True).color(ROSE).label(f'IN−\n(electrode −)', loc='left')
        elm.Resistor().right().color(ROSE).label('R_PROT\n10kΩ', loc='top')
        ina_inn = d.here

        # ── INA128 ─────────────────────────────────────────────────────────────
        d.pop()
        ina = (elm.Ic(pins=[
            elm.IcPin(name='IN+',  pin='3',  side='left'),
            elm.IcPin(name='IN−',  pin='2',  side='left',  anchorname='inn'),
            elm.IcPin(name='RG1',  pin='1',  side='bot',   anchorname='rg1'),
            elm.IcPin(name='RG2',  pin='8',  side='bot',   anchorname='rg2'),
            elm.IcPin(name='REF',  pin='5',  side='bot',   anchorname='ref'),
            elm.IcPin(name='V+',   pin='7',  side='top',   anchorname='vp'),
            elm.IcPin(name='V−',   pin='4',  side='bot',   anchorname='vm'),
            elm.IcPin(name='OUT',  pin='6',  side='right'),
        ], edgepadH=0.5, edgepadW=1.0, pinspacing=1.0)
               .color(CYAN)
               .right()
               .anchor('inIN+')
               .at(ina_inp)
               .label('U1\nINA128PA\nG=51', loc='center'))

        # Gain resistor RG
        rg_start = ina.rg1
        d.push()
        elm.Line().at(rg_start).down().length(0.5).color(CYAN)
        rg = elm.Resistor().right().label('R_G\n1kΩ 1%', loc='top').color(CYAN)
        elm.Line().up().length(0.5).color(CYAN)
        elm.Wire('|-').to(ina.rg2).color(CYAN)
        d.pop()

        # REF pin → 1.65V
        d.push()
        elm.Line().at(ina.ref).down().length(0.8).color(VIOLET)
        elm.Label().label('→ 1.65V REF\n(shared all ch)', loc='right').color(VIOLET)
        d.pop()

        # V+ supply
        d.push()
        elm.Line().at(ina.vp).up().length(0.5).color(AMBER)
        elm.Label().label('+5V', loc='right').color(AMBER)
        d.pop()

        ina_out = ina.OUT

        # ── Sallen-Key HPF (fc=20Hz) ────────────────────────────────────────────
        d.push()
        elm.Line().at(ina_out).right().length(0.5).color(EMERALD)
        c_hpf1 = elm.Capacitor().right().label('C_HPF\n470nF film', loc='top').color(EMERALD)
        r_hpf1 = elm.Resistor().right().label('R_HPF1\n12kΩ', loc='top').color(EMERALD)
        hpf_mid = d.here  # junction

        # Second cap to ground
        d.push()
        elm.Capacitor().down().label('C_HPF2\n470nF film', loc='right').color(EMERALD)
        elm.Ground().color(EMERALD)
        d.pop()

        r_hpf2 = elm.Resistor().right().label('R_HPF2\n24kΩ', loc='top').color(EMERALD)
        hpf_out = d.here

        # Op-amp buffer U2a
        hpf_opamp = (elm.Opamp()
                     .anchor('in1')
                     .at(hpf_out)
                     .right()
                     .color(EMERALD)
                     .label('U2a\nMCP6002', loc='center'))
        # Feedback (unity gain)
        elm.Line().at(hpf_opamp.out).right().length(0.3).color(EMERALD)
        elm.Wire('n|-').to(hpf_opamp.in2).color(EMERALD)
        hpf_final = hpf_opamp.out
        d.pop()

        # ── Twin-T Notch (50Hz) ─────────────────────────────────────────────────
        d.push()
        elm.Line().at(hpf_final).right().length(0.4).color(AMBER)

        # Upper T: R-R-C
        d.push()
        elm.Resistor().right().label('R_N1\n10kΩ 1%', loc='top').color(AMBER)
        notch_mid_top = d.here
        elm.Resistor().right().label('R_N2\n10kΩ 1%', loc='top').color(AMBER)
        notch_out = d.here
        d.pop()

        # Lower T: C-C-R
        d.push()
        elm.Line().down().length(0.8).color(AMBER)
        elm.Capacitor().right().label('C_N1\n330nF 1%', loc='bot').color(AMBER)
        d.push()
        elm.Resistor().down().label('R_N_HALF\n5.1kΩ+trim', loc='right').color(AMBER)
        elm.Ground().color(AMBER)
        d.pop()
        elm.Capacitor().right().label('C_N2\n330nF 1%', loc='bot').color(AMBER)
        d.pop()

        # Double cap to ground (2C = 680nF)
        d.push()
        elm.Line().at(notch_mid_top).down().length(0.4).color(AMBER)
        elm.Capacitor().down().label('2C=680nF 1%', loc='right').color(AMBER)
        elm.Ground().color(AMBER)
        d.pop()

        elm.Line().at(notch_out).right().length(0.3).color(AMBER)
        notch_final = d.here
        d.pop()

        # ── Variable Gain (MCP6002) ─────────────────────────────────────────────
        d.push()
        elm.Line().at(notch_final).right().length(0.4).color(VIOLET)
        vg_opamp = (elm.Opamp()
                    .anchor('in1')
                    .right()
                    .color(VIOLET)
                    .label('U3\nMCP6002\nVar Gain', loc='center'))

        # Potentiometer feedback
        d.push()
        elm.Line().at(vg_opamp.out).right().length(0.3).color(VIOLET)
        vg_out = d.here
        elm.Line().up().length(0.5).color(VIOLET)
        pot = elm.ResistorVar().left().label('RV1\n100kΩ B100K', loc='top').color(VIOLET)
        elm.Wire('|-').to(vg_opamp.in2).color(VIOLET)
        d.pop()

        # Series protection at ADC
        elm.Line().at(vg_out).right().length(0.3).color(CYAN)
        elm.Resistor().right().label('R_AA\n1kΩ', loc='top').color(CYAN)
        adc_in = d.here
        d.push()
        elm.Capacitor().down().label('33nF', loc='right').color(CYAN)
        elm.Ground().color(CYAN)
        d.pop()

        # ── ESP32-S3 ────────────────────────────────────────────────────────────
        elm.Dot(open=True).color(CYAN)
        elm.Label().label(f'GPIO{channel_num}\n(ADC1_CH{channel_num-1})\nESP32-S3', loc='right').color(CYAN)
        d.pop()

        # Stage labels at top
        fig = d.get_figure()
        ax  = fig.axes[0]
        ax.set_facecolor(BG)
        fig.patch.set_facecolor(BG)

        # Title
        ax.set_title(
            f'sEMG AFE — Single Channel (CH{channel_num})\n'
            f'INA128 G=51 → BPF 20-100Hz → Twin-T 50Hz Notch → Variable Gain → ESP32-S3 ADC1',
            color=FG, fontsize=11, pad=12, fontweight='bold'
        )

        out_png = OUT / f'semg_ch{channel_num}_schematic.png'
        out_svg = OUT / f'semg_ch{channel_num}_schematic.svg'
        fig.savefig(str(out_png), dpi=200, bbox_inches='tight',
                    facecolor=BG, edgecolor='none')
        fig.savefig(str(out_svg), bbox_inches='tight',
                    facecolor=BG, edgecolor='none')
        plt.close(fig)
        print(f"Saved: {out_png}")
        print(f"Saved: {out_svg}")
        return str(out_png)


if __name__ == '__main__':
    print("Generating sEMG AFE schematic diagram...")
    path = draw_one_channel(channel_num=1)
    print(f"\nDone. Open: {path}")
    print("\nNote: This shows CH1 completely wired.")
    print("CH2-CH4 are identical — same circuit, different GPIO pin (GPIO2, GPIO3, GPIO4).")
    print("The 1.65V REF buffer (shared) is a single MCP6002 unity follower")
    print("connected to ALL 4x INA128 REF pins simultaneously.")
