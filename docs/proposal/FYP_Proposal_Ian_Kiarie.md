# PROJECT PROPOSAL

## Design and Implementation of a Low-Cost Multichannel Surface Electromyography Control Interface for Upper-Limb Prosthetic Rehabilitation

**Student:** Ian Kiarie Kabura
**Registration Number:** ENE212-0069/2022
**Department:** Electronic and Computer Engineering
**Programme:** B.Sc. Electronic and Computer Engineering
**Supervisor:** Dr Irene Muisyo
**Date:** October 2026

> **Note:** This markdown mirrors the revised DOCX (ENE212-0069-2022_PROJECT_PROPOSAL_SEP-2026_REVISED.docx).
> The DOCX is the submission copy; this markdown is for version control and reference.

---

## ABSTRACT

Access to upper-limb prosthetic technologies in sub-Saharan Africa is constrained by cost, with commercial myoelectric control interfaces priced between USD 3,000 and USD 8,000. This project proposes the design and implementation of a low-cost, four-channel surface electromyography (sEMG) control interface for upper-limb prosthetic rehabilitation. The system captures residual-limb muscle activity through surface electrodes, conditions the signals using a dedicated analog front-end built around INA128 instrumentation amplifiers with Sallen-Key bandpass filtering (20--500 Hz) and a 50 Hz twin-T notch stage, and samples four channels at 2 kHz on an ESP32-S3 microcontroller. Time-domain features are extracted from 250 ms analysis windows and classified into eight hand gestures using a compact 1D convolutional neural network quantized to INT8. The classified gestures drive a 2-DOF 3D-printed prosthetic gripper, while a Bluetooth Low Energy (BLE) mobile application provides guided user calibration and live monitoring. The system targets a signal-to-noise ratio above 20 dB, macro F1 score of 0.85 or higher, inference latency below 50 ms, and an electronics bill of materials below USD 20.

---

## CHAPTER ONE: INTRODUCTION

### 1.1 Background of the Study

Surface electromyography (sEMG) is a non-invasive technique for observing electrical activity associated with voluntary skeletal-muscle contraction. Motor-unit action potentials produce small voltage variations at the skin surface; after amplification and filtering, these signals can be analysed to infer intended movement. Typical sEMG energy is concentrated within the 20--500 Hz range, with raw amplitudes between 50 uV and 5 mV, while practical measurements are affected by 50 Hz power-line interference, motion artefacts below 20 Hz, electrode-skin impedance variations, and thermal noise [1], [6].

For myoelectric prosthetic control, surface electrodes positioned over residual-limb musculature capture sEMG signals during voluntary contractions. A signal-conditioning and classification pipeline amplifies the differential signal, suppresses unwanted frequency components, samples the conditioned waveform, computes discriminative features, and maps the resulting feature vector to a hand gesture. Multichannel acquisition (4--8 channels) provides spatial diversity essential for distinguishing more than 2--3 gestures reliably [6], [7].

Commercial myoelectric prostheses achieve >95% accuracy but cost USD 10,000--40,000, with the control electronics alone at USD 3,000--8,000. Research prototypes cost USD 500--1,500 for electronics [4]. Globally, ~30 million people require prosthetic/orthotic devices, with sub-Saharan Africa at ~0.5% prevalence [10]. In Kenya, KNH fits <200 powered prosthetic arms/year.

Three recent developments make a low-cost system feasible: (1) ESP32-S3 at ~USD 5 with TFLite Micro support; (2) TensorFlow Lite for Microcontrollers enabling sub-100 KB models; (3) Published ESP32 sEMG classification with <10 ms inference [1].

Four channels represent a practical trade-off: 4--8 channels is optimal for 6--8 gesture classification [6], [7]. Beyond 8 channels, gains diminish while cost/complexity increase.

### 1.2 Problem Statement

The electronic control interface for myoelectric prosthetics remains prohibitively expensive for developing economies. Within the literature, low-cost prototypes commonly address only parts of the pipeline: single-channel classification without prosthetic integration [1]; high accuracy requiring GPU inference [2]; or prosthetic actuation with >USD 500 electronics [4]. No published system combines multichannel acquisition, on-device CNN classification, user calibration, and prosthetic actuation for <USD 20 component cost.

**Research question:** Can a complete, 4-channel myoelectric control system -- from skin electrode to mechanical grip -- be designed, built, and validated for under USD 20 in component cost?

### 1.3 Project Justification

A low-cost multichannel sEMG control interface will: reduce the electronics cost barrier from hundreds of dollars to <USD 20; enable standalone wearable operation without PC connection; adapt to individual users via BLE calibration in ~3 minutes; validate end-to-end feasibility (not classification in isolation); and create a reproducible open-source platform for other researchers.

### 1.4 Scope of the Study

**In scope:** 4-channel sEMG acquisition (INA128), analog conditioning (bandpass + notch), ESP32-S3 DSP at 2 kHz/ch, time-domain features, INT8 1D-CNN classification, BLE calibration, 2-DOF gripper actuation, 5-subject validation with ethics approval.

**Out of scope:** Large-scale deployment, clinical trials, frequency-domain features, >8 gestures, medical device certification, high-density electrode arrays. The prototype is a research demonstrator, not a certified prosthesis.

### 1.5 Objectives

#### 1.5.1 Main Objective

To design, implement, and validate a low-cost, 4-channel sEMG acquisition and gesture-classification system on an ESP32-S3, targeting >80% real-time accuracy, <50 ms inference latency, and <USD 20 electronics BOM, demonstrated through a 2-DOF prosthetic gripper.

#### 1.5.2 Specific Objectives

1. To design a 4-channel analog front-end using INA128 (gain=50), Sallen-Key bandpass (20--500 Hz), and twin-T 50 Hz notch, targeting SNR >20 dB and crosstalk <-40 dB.
2. To implement a real-time embedded signal processing and gesture classification pipeline on the ESP32-S3, including 2 kHz/ch sampling, 24-element feature extraction, and INT8 1D-CNN deployment (<100 KB) via TFLite Micro, achieving offline macro F1 >= 0.85.
3. To test and evaluate the complete system by integrating with a PCA9685-driven 2-DOF gripper, validating with 5 subjects, measuring real-time accuracy, actuation correctness, latency, and cost against the USD 20 BOM target.

---

## CHAPTER TWO: LITERATURE REVIEW

### 2.1 Overview

This chapter reviews technical literature organised by themes following the signal pipeline from electrode to actuator. Each theme justifies a design choice in the methodology.

### 2.2 sEMG Signal Characteristics, Electrode Placement, and Acquisition

sEMG captures motor-unit action potentials through skin surface electrodes (50 uV -- 5 mV, 20--500 Hz). Differential placement over flexor carpi radialis, extensor digitorum, flexor digitorum superficialis, and brachioradialis provides spatial diversity for hand gesture recognition [6], [7].

### 2.3 Analog Front-End Design: Amplification, Filtering, and Noise Reduction

INA128 provides CMRR >120 dB. Gain must be split: G=50 at INA128 (not 500, which saturates from DC offset), then HPF removes DC, then variable post-filter gain (4--10x) for total 200--500x. Sallen-Key topology for bandpass, twin-T for 50 Hz notch. MCP6002 for single-supply operation [6].

### 2.4 Signal Segmentation, Feature Extraction, and Preprocessing

250 ms window, 50 ms step (80% overlap). Six time-domain features per channel: MAV, RMS, WL, ZC, SSC, Variance. These compete with frequency-domain features for 6--8 gesture tasks [6], [7].

### 2.5 Machine-Learning Methods for sEMG Gesture Classification

Salgado et al. [1]: single-channel ESP32, 4 gestures, 92% accuracy, <10 ms inference. Mekruksavanich et al. [2]: deep residual network, 97% on NinaPro DB5, but GPU-only. CNN outperforms SVM/LDA beyond 4 classes [6]. INT8 quantisation reduces model 4x.

### 2.6 Embedded and TinyML Deployment

TFLite Micro supports ESP32-S3 (512 KB SRAM, 8 MB PSRAM). The 1D-CNN operates on 24-element feature vectors (not raw time series), keeping input compact and inference fast [1].

### 2.7 User Calibration and Personalisation

Lee et al. [3]: FedEMG achieves personalisation with ~2 min calibration. Fine-tuning the last dense layer via transfer learning is sufficient [3].

### 2.8 Prosthetic Actuation and Control Interfaces

Furtado et al. [5]: 2--3 DOF sufficient for 80% of daily activities. Molinari et al. [4]: real-time control but >USD 500. PCA9685 + servo motors with confidence filtering and debounce for this project.

### 2.9 Cost and Accessibility

Three tiers defined: Tier A (electronics BOM), Tier B (prototype), Tier C (volume). USD 20 target compared against Molinari >USD 500 and commercial USD 3,000--8,000.

### 2.10 Critique of Existing Literature

No reviewed system integrates all four: multichannel acquisition, on-device CNN, user personalisation, and prosthetic actuation at <USD 20.

| Study | Channels | On-Device ML | Prosthetic | Cost |
|---|---|---|---|---|
| Salgado et al. [1] | 1 | Yes (ESP32) | No | Not reported |
| Mekruksavanich et al. [2] | 16 | No (GPU) | No | N/A |
| Lee et al. [3] | Multiple | Edge-capable | No | N/A |
| Molinari et al. [4] | 64 | FPGA | Yes | >USD 500 |
| Furtado et al. [5] | sEMG | Partial | Yes | "Low-cost" |
| **This project** | **4** | **Yes (ESP32-S3)** | **Yes** | **<USD 20** |

### 2.11 Proposed Work

A 4-channel sEMG control interface with on-device 1D-CNN on ESP32-S3, driving a 2-DOF gripper, at USD 20 electronics BOM. Integrates amplification, filtering, DSP, INT8 classification, BLE calibration, and servo actuation in a battery-powered platform.

---

## CHAPTER THREE: METHODOLOGY

### 3.1 Overview

Methodology structured by the 3 specific objectives: hardware design (3.2), embedded processing and ML (3.3), and system testing (3.4--3.7).

### 3.2 System Architecture and Hardware Design

| Parameter | Specification |
|---|---|
| Signal type | Surface EMG, differential |
| Channels | 4 (FCR, ED, FDS, BR) |
| Raw amplitude | 50 uV -- 5 mV |
| Frequency band | 20--500 Hz + 50 Hz notch |
| Sampling rate | 2 kHz/channel |
| Window | 250 ms (500 samples), 50 ms step |
| Features/channel | MAV, RMS, WL, ZC, SSC, Variance |
| Feature vector | 24 elements |

#### 3.2.1 Analog Front-End Design

- INA128 gain: G = 1 + 50k/R_G = 50, R_G = 1.02 kOhm
- HPF: Sallen-Key, f_c=20 Hz, C=100 nF, R=82 kOhm
- LPF: Sallen-Key, f_c=500 Hz, C=10 nF, R=33 kOhm
- Notch: twin-T, R=33 kOhm, C=100 nF, f=48.2 Hz (~50 Hz with tolerance)
- Variable gain: 4--10x post-notch, total 200--500x

#### 3.2.2 Electrical Safety

Battery-powered (9V, no mains). INA128 bias current <1 uA. 10 kOhm current-limiting resistors on electrode leads (<50 uA fault current). Leakage test before human testing. Ethics approval + informed consent required.

#### 3.2.3 SNR and Crosstalk Measurement

SNR: inject 100 Hz sinusoid, measure with oscilloscope, target >20 dB. Crosstalk: inject signal on one channel, measure coupling on others, target <-40 dB.

### 3.3 Signal Processing and Machine Learning

#### 3.3.1 Data Path

ADC (DMA, 2 kHz x 4ch) -> 250 ms window -> 6 features/ch -> normalise -> INT8 1D-CNN -> gesture class

#### 3.3.2 Model Architecture and Training

- Input: 24-element feature vector
- Architecture: Conv1D(16) -> BN -> ReLU -> Conv1D(32) -> BN -> ReLU -> GAP -> Dense(64) -> Softmax(8)
- Training data: NinaPro DB5, 10 subjects, channels [0,4,8,12], 50-sample windows at 200 Hz
- Rest class undersampled to match mean gesture count
- Split: 70/15/15 train/val/test, subject-independent
- Baselines: LDA, SVM, RF
- Quantize to INT8, target <100 KB

#### 3.3.3 Calibration and Participant Testing

BLE calibration: 8 gestures x 3 reps x 5 s + 3 s rest = ~3 min. Transfer learning fine-tunes last dense layer.

5-subject evaluation: healthy adults (18--35), 5 reps x 8 gestures after calibration. Ethics approval + consent.

#### 3.3.4 Metrics Definition

- **Accuracy:** correct/total trials
- **Macro F1:** unweighted average of per-class F1
- **Actuation correctness:** correct gripper motions/classified gestures
- **Latency (3 components):** (1) model inference time (<50 ms target), (2) decision time (filtering + debounce), (3) total system response (window + inference + servo)

### 3.4 Actuation and End-to-End Integration

Gesture-to-gripper mapping:
- Rest: no change
- Open Hand: full open
- Power Grasp: full close
- Pinch Grip: partial close
- Point: open (control command -- mode change)
- Wrist Flexion: rotate down
- Wrist Extension: rotate up
- Thumbs Up: open (mode selection toggle)

Safety: confidence threshold (0.7), temporal debounce (2 consecutive), command hold (500 ms), emergency stop (physical + BLE).

### 3.5 Ethics, Safety, and Data

Ethics approval + informed consent. Not a medical device. Data pseudonymised. Physical power-disconnect. Withdrawal without penalty.

### 3.6 Cost Evaluation

| Tier | Contents | Target |
|---|---|---|
| A: Electronics BOM | ICs, passives, connectors | <USD 20 |
| B: Prototype BOM | Tier A + PCB, 3D print, batteries, servos | <USD 65 |
| C: Volume (100+) | Tier A at volume pricing | <USD 15 |

### 3.7 Performance Targets

| Layer | Metric | Target |
|---|---|---|
| Electronics | SNR | >20 dB |
| Electronics | Crosstalk | <-40 dB |
| Electronics | 50 Hz rejection | >30 dB |
| ML (offline) | Macro F1 | >=0.85 |
| Embedded ML | Model size | <100 KB |
| Real-time | Inference latency | <50 ms |
| System | Mean accuracy (5 subj) | >=80% |
| Cost | Tier A BOM | <USD 20 |

---

## CHAPTER FOUR: EXPECTED RESULTS

### 4.1 Expected Electrical Performance (Objective 1)

SNR >20 dB, bandpass -3 dB at ~20/500 Hz, notch >30 dB, crosstalk <-40 dB. Informed by simulation (Sallen-Key: 22/492 Hz; notch: 32.3 dB).

### 4.2 Expected Embedded Performance (Objective 2)

Stable 4-ch 2 kHz sampling via DMA. Feature extraction <5 ms. INT8 model <100 KB, <50 KB SRAM, <50 ms inference. Total pipeline RAM <200 KB.

### 4.3 Expected Classification Performance (Objective 2)

Offline macro F1 >= 0.85 (conservative vs Mekruksavanich 97%, accounting for 4-ch, feature-based, INT8). Baselines expected F1 0.70--0.80.

### 4.4 Expected Calibration Improvement (Objective 2)

+10 percentage points from transfer learning [3].

### 4.5 Expected End-to-End Performance (Objective 3)

Mean real-time accuracy >=80% across 5 subjects. Actuation correctness within 5 pp of classification accuracy. Total response ~300 ms. Statistical claims limited to descriptive stats (5-subject limitation stated).

### 4.6 Expected Cost Outcome (Objective 3)

Tier A <USD 20 (~KES 2,600). Tier B <USD 65 (~KES 8,450). Compared against Molinari >USD 500 and commercial USD 3,000--8,000.

---

## PROJECT BUDGET

| # | Item | Qty | Rate (KES) | Amount (KES) |
|---|---|---|---|---|
| 1 | ESP32-S3-DevKitC-1 | 1 | 800 | 800 |
| 2 | INA128 Instrumentation Amplifier | 4 | 350 | 1,400 |
| 3 | MCP6002 Op-Amp | 8 | 50 | 400 |
| 4 | Passive components | 1 set | 300 | 300 |
| 5 | Ag/AgCl Electrodes (50 pack) | 1 | 500 | 500 |
| 6 | PCA9685 PWM Driver | 1 | 250 | 250 |
| 7 | MG996R Servo Motors | 2 | 400 | 800 |
| 8 | 3D Printing (PETG) | 1 | 1,500 | 1,500 |
| 9 | 4-Layer PCB (JLCPCB + shipping) | 1 | 1,200 | 1,200 |
| 10 | 9V Batteries + Holders | 2 | 150 | 300 |
| 11 | Connectors, wires, enclosure | 1 set | 500 | 500 |
| 12 | Electrode cables, shielded leads | 4 | 100 | 400 |
| | | | **TOTAL** | **KES 8,350 (~USD 64)** |

Tier A (items 1--4, 6): KES 2,950 (~USD 23). Tier B (all): KES 8,350 (~USD 64).

---

## PROJECT TIMEPLAN

| Activity | SEP | OCT | NOV | DEC | JAN | FEB | MAR | APR | MAY |
|---|---|---|---|---|---|---|---|---|---|
| Proposal Writing | X | X | | | | | | | |
| Literature Review | X | X | X | | | | | | |
| Proposal Presentation | | X | | | | | | | |
| Simulation & Single-Ch PoC | X | X | X | | | | | | |
| PCB Design & Fabrication | | | X | X | X | | | | |
| ML Training & Deployment | | | X | X | X | | | | |
| BLE App Development | | | | X | X | X | | | |
| System Integration | | | | | | X | X | | |
| 5-Subject Validation | | | | | | | X | | |
| Final Report Writing | | | | | | | X | X | |
| Final Presentation | | | | | | | | | X |

---

## REFERENCES

[1] J. A. M. Salgado et al., "Single-channel sEMG hand gesture classification using an ANN implemented on an ESP32 microcontroller," IEEE Access, vol. 13, pp. 28756-28768, 2025.
[2] S. Mekruksavanich et al., "Deep residual network with channel attention for improving hand gesture recognition with sEMG signal," IEEE Access, vol. 14, 2026.
[3] H. Lee et al., "FedEMG: Achieving generalization, personalization, and resource efficiency in EMG-based upper-limb rehabilitation through federated prototype learning," IEEE Trans. NSRE, vol. 33, 2025.
[4] R. G. Molinari et al., "A wearable platform for real-time control of a prosthetic hand by high-density EMG," IEEE Access, vol. 14, 2026.
[5] D. M. Furtado et al., "A modular low-cost myoelectric prosthetic hand," Proc. IEEE IFEE, 2026.
[6] P. Prakash et al., "Advances in EMG, EEG, and hybrid-based prosthetic arm control," Arch. Comput. Methods Eng., 2026.
[7] B. Abdikenov et al., "Emerging frontiers in robotic upper-limb prostheses," Sensors, vol. 25, no. 13, p. 3892, 2025.
[8] F. Del Pup et al., "Multimodal surface EMG hand gesture recognition using query-based transformers for prosthetic control," arXiv:2607.22779, 2026.
[9] R. Geddam et al., "sEMGCareHCI: sEMG-based finger position prediction for amputee-centric HCI," npj Biomedical Innovations, 2026.
[10] WHO, "Global Report on Assistive Technology," 2022.
