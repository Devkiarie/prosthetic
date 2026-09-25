#!/usr/bin/env python3
"""
plot_sims.py — Bode plot viewer for FYP sEMG AFE simulations
Usage: cd hardware/simulation && python3 plot_sims.py
Requires: matplotlib, spyci (pip3 install spyci)
Run simulations first: bash run_all_sims.sh
"""
import sys
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
from pathlib import Path

try:
    from spyci.spyci import load_raw
except ImportError:
    print("Missing: pip3 install spyci")
    sys.exit(1)

SIM_DIR = Path(__file__).parent

def get_vec(data, name):
    """Extract a named variable from spyci structured array as complex numpy array."""
    return np.array(data['values'][name], dtype=complex)

def get_db(data, name):
    """Return |V| in dB for a named node."""
    return 20 * np.log10(np.abs(get_vec(data, name)) + 1e-14)

def load(filename):
    path = SIM_DIR / filename
    if not path.exists():
        print(f"  [!] {filename} missing — run: bash run_all_sims.sh")
        return None
    return load_raw(str(path))

# ── Load all raw files ────────────────────────────────────────────────
d1 = load('sim01_ina128.raw')
d2 = load('sim02_bandpass.raw')
d3 = load('sim03_notch.raw')
d4 = load('sim04_full_chain.raw')

fig = plt.figure(figsize=(14, 10))
fig.suptitle('sEMG AFE — Bode Plots  |  FYP Ian Kiarie, JKUAT 2026', 
             fontsize=13, fontweight='bold')
gs = gridspec.GridSpec(2, 2, hspace=0.48, wspace=0.35)

# ── Panel 1: INA128 ───────────────────────────────────────────────────
ax1 = fig.add_subplot(gs[0, 0])
ax1.set_title('SIM 01 — INA128  G=50')
if d1:
    freq = np.abs(get_vec(d1, 'frequency'))
    db   = get_db(d1, 'v(out)')
    ax1.semilogx(freq, db, color='steelblue', lw=2)
    g100 = db[np.argmin(np.abs(freq - 100))]
    ax1.axhline(g100 - 3, color='red', lw=0.8, ls='--', label='−3 dB')
    ax1.annotate(f'{g100:.1f} dBV @ 100 Hz\n(50 mV out / 1 mV in = 50×)',
                 xy=(100, g100), xytext=(500, g100 - 10),
                 fontsize=7, color='steelblue',
                 arrowprops=dict(arrowstyle='->', color='steelblue', lw=0.8))
    ax1.set(xlabel='Frequency (Hz)', ylabel='Gain (dBV)',
            xlim=(1, 1e6))
    ax1.grid(True, which='both', alpha=0.3)
    ax1.legend(fontsize=8)

# ── Panel 2: Sallen-Key Bandpass ────────────────────────────────────
ax2 = fig.add_subplot(gs[0, 1])
ax2.set_title('SIM 02 — Sallen-Key Bandpass 20–500 Hz')
if d2:
    freq = np.abs(get_vec(d2, 'frequency'))
    db   = get_db(d2, 'v(nlpf_out)')
    ax2.semilogx(freq, db, color='forestgreen', lw=2)
    peak = np.max(db)
    ax2.axhline(peak - 3, color='red', lw=0.8, ls='--', label='−3 dB')
    ax2.axvline(20,  color='orange', lw=1, ls=':', alpha=0.8, label='20 Hz')
    ax2.axvline(500, color='purple', lw=1, ls=':', alpha=0.8, label='500 Hz')
    # Annotate -3 dB points
    for target, side in [(20, 'L'), (500, 'H')]:
        idx = np.argmin(np.abs(freq - target))
        ax2.annotate(f'{db[idx]:.1f} dBV', xy=(freq[idx], db[idx]),
                     fontsize=7, color='gray',
                     xytext=(freq[idx]*1.4, db[idx]+1.5))
    ax2.set(xlabel='Frequency (Hz)', ylabel='Gain (dB)', xlim=(1, 10000))
    ax2.grid(True, which='both', alpha=0.3)
    ax2.legend(fontsize=7)

# ── Panel 3: Twin-T Notch ─────────────────────────────────────────────
ax3 = fig.add_subplot(gs[1, 0])
ax3.set_title('SIM 03 — Twin-T Notch  50 Hz')
if d3:
    freq = np.abs(get_vec(d3, 'frequency'))
    db   = get_db(d3, 'v(notch_out)')
    ax3.semilogx(freq, db, color='darkorange', lw=2)
    ax3.axvline(50, color='red', lw=1, ls='--', label='50 Hz target')
    ax3.axhline(-30, color='gray', lw=0.8, ls=':', label='−30 dB pass criterion')
    idx50 = np.argmin(np.abs(freq - 50))
    ax3.annotate(f'Notch: {db[idx50]:.1f} dB\n(pass: < −30 dB)',
                 xy=(50, db[idx50]), xytext=(80, db[idx50] + 12),
                 fontsize=7.5, color='red',
                 arrowprops=dict(arrowstyle='->', color='red', lw=0.8))
    ax3.set(xlabel='Frequency (Hz)', ylabel='Gain (dB)', xlim=(10, 1000))
    ax3.grid(True, which='both', alpha=0.3)
    ax3.legend(fontsize=8)

# ── Panel 4: Full Signal Chain ────────────────────────────────────────
ax4 = fig.add_subplot(gs[1, 1])
ax4.set_title('SIM 04 — Full Chain  INA→HPF→LPF→Notch→×6')
if d4:
    freq = np.abs(get_vec(d4, 'frequency'))
    # All stage outputs
    curves = [
        ('v(ina_out)',    'INA128 out',  'C0', ':'),
        ('v(nhpf_out)',   'After HPF',   'C2', ':'),
        ('v(nlpf_out)',   'After LPF',   'C3', ':'),
        ('v(notch_out)',  'After Notch', 'C4', '--'),
        ('v(vgain_out)',  'Final out',   'crimson', '-'),
    ]
    for node, label, color, ls in curves:
        db = get_db(d4, node)
        ax4.semilogx(freq, db, color=color, lw=1.5 if ls == '-' else 0.9,
                     ls=ls, label=label, alpha=1.0 if ls == '-' else 0.6)
    # Notch marker
    db_out = get_db(d4, 'v(vgain_out)')
    idx50 = np.argmin(np.abs(freq - 50))
    ax4.annotate(f'Notch\n{db_out[idx50]:.1f} dBV',
                 xy=(50, db_out[idx50]), xytext=(90, db_out[idx50] - 6),
                 fontsize=7, color='crimson',
                 arrowprops=dict(arrowstyle='->', color='crimson', lw=0.8))
    ax4.axvline(20,  color='orange', lw=0.8, ls=':', alpha=0.6)
    ax4.axvline(500, color='orange', lw=0.8, ls=':', alpha=0.6, label='20/500 Hz')
    ax4.set(xlabel='Frequency (Hz)', ylabel='Gain (dBV, input=1mV)', xlim=(1, 10000))
    ax4.grid(True, which='both', alpha=0.3)
    ax4.legend(fontsize=7, loc='lower left')

out_path = SIM_DIR / 'bode_plots.png'
plt.savefig(str(out_path), dpi=150, bbox_inches='tight')
print(f"Saved: {out_path}")
plt.show()
