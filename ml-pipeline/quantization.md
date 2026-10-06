# ML: Quantization

**Status:** DONE — 2026-10-06
**Related module:** [[modules/gesture-classifier]]
**Plan step:** [[docs/COMPREHENSIVE_PLAN]]

## Results

| Metric | Value |
|---|---|
| Model | MLP no-BatchNorm (semg_mlp_nobn_float32.keras rebuilt) |
| Float32 F1 | 0.7907 |
| INT8 F1 | 0.7845 |
| F1 drop | 0.0062 |
| Size | 123.7 KB |
| ESP32-S3 SRAM fit | PASS (123.7 KB / 512 KB = 24%) |
| Output file | `ml/models/semg_model_int8.tflite` |

## Method

TFLite INT8 via Keras 3 was blocked by `LLVM ERROR: unresolved type conflict` (MLIR
BatchNorm bug) on all paths: `from_keras_model`, `from_concrete_functions`, TOCO,
`disable_experimental_new_converter`.

**Fix:** Rebuilt the model in a fresh Keras graph by extracting Dense layer weights
from the saved `.keras` file and copying them into a new `tf.keras.Model` with no
BatchNorm layers. Quantized that clean model with `MANIFOLD` solver and INT8 ops.

```python
os.environ["TF_USE_LEGACY_KERAS"] = "1"
conv = tf.lite.TFLiteConverter.from_keras_model(model_new)
conv.optimizations = [tf.lite.Optimize.DEFAULT]
conv.representative_dataset = rep
conv.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
conv.inference_input_type  = tf.int8
conv.inference_output_type = tf.int8
```

## Files

- Float32 model: `ml/models/semg_mlp_nobn_float32.keras`
- INT8 TFLite:   `ml/models/semg_model_int8.tflite`
- C header:      `ml/models/semg_weights.h` (float32, for direct ESP32 use)
- Script:        `/tmp/quantize_fix.py` (rebuild + quantize)

## Log

- 2026-10-06: INT8 blocked (Keras3 MLIR LLVM ERROR). Float32 C header exported as interim.
- 2026-10-06: tf-keras installed. Keras3 load + weight copy + re-quantize worked.
              INT8 F1=0.7845, size=123.7 KB. Committed `71472bf`.
