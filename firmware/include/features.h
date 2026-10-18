/**
 * features.h — Time-domain feature extraction for sEMG
 * sEMG Prosthetic Hand | FYP ENE212-0069/2022 | Ian Kiarie
 *
 * Extracts 6 features × 4 channels = 24-element feature vector from a
 * 500-sample (250ms @ 2kHz) window of 4-channel ADC data.
 *
 * Features: MAV, RMS, WL, ZC, SSC, VAR
 * Reference: Hudgins et al. 1993, IEEE Trans Biomed Eng 40(2):82-92
 */
#pragma once
#include <cmath>
#include <cstdint>
#include "config.h"

/**
 * Compute 24 time-domain features from one window.
 *
 * @param window  Raw ADC window [WIN_SAMPLES][N_CH], values 0–4095
 * @param feats   Output array of 24 floats (normalised 0-centred via caller)
 */
void extract_features(const int16_t window[WIN_SAMPLES][N_CH], float feats[FEAT_SIZE]) {
    // Convert ADC raw (0-4095, 12-bit, 0.15-2.45V usable) to voltage, then
    // centre around 1.65V REF so the signal swings ±Vpeak around 0.
    const float adc_scale = (ADC_VMAX - ADC_VMIN) / (1 << ADC_BITS);

    for (int ch = 0; ch < N_CH; ch++) {
        float samples[WIN_SAMPLES];
        float max_abs = 0.0f;

        // Convert and centre
        for (int i = 0; i < WIN_SAMPLES; i++) {
            float v = window[i][ch] * adc_scale + ADC_VMIN - ADC_VMID;
            samples[i] = v;
            if (fabsf(v) > max_abs) max_abs = fabsf(v);
        }

        // Threshold for ZC / SSC (1% of max amplitude)
        const float thr = 0.01f * (max_abs + 1e-9f);
        const int   N   = WIN_SAMPLES;

        // MAV — Mean Absolute Value
        float sum_abs = 0.0f;
        for (int i = 0; i < N; i++) sum_abs += fabsf(samples[i]);
        float mav = sum_abs / N;

        // RMS — Root Mean Square
        float sum_sq = 0.0f;
        for (int i = 0; i < N; i++) sum_sq += samples[i] * samples[i];
        float rms = sqrtf(sum_sq / N);

        // WL — Waveform Length
        float wl = 0.0f;
        for (int i = 1; i < N; i++) wl += fabsf(samples[i] - samples[i-1]);

        // ZC — Zero Crossings (with threshold)
        int zc = 0;
        for (int i = 1; i < N; i++) {
            if (samples[i-1] * samples[i] < 0.0f &&
                fabsf(samples[i-1] - samples[i]) > thr) zc++;
        }

        // SSC — Slope Sign Changes (with threshold)
        int ssc = 0;
        for (int i = 1; i < N-1; i++) {
            float d1 = samples[i] - samples[i-1];
            float d2 = samples[i] - samples[i+1];
            if (d1 * d2 > 0.0f &&
                (fabsf(d1) > thr || fabsf(d2) > thr)) ssc++;
        }

        // VAR — Variance
        float var = (sum_sq / N) - (mav * mav);
        if (var < 0.0f) var = 0.0f;  // numerical guard

        // Store 6 features for this channel (channel-major order)
        int base = ch * N_FEAT_PER_CH;
        feats[base + 0] = mav;
        feats[base + 1] = rms;
        feats[base + 2] = wl;
        feats[base + 3] = static_cast<float>(zc);
        feats[base + 4] = static_cast<float>(ssc);
        feats[base + 5] = var;
    }
}
