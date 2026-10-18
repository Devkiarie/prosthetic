# ML: INT8 Quantization

**Status:** DONE -- 2026-10-18 (train_v3.py slim model)
**Related module:** [[modules/gesture-classifier]]
**Model:** `firmware/model/semg_v3_int8.tflite`

## Results (final, verified)

| Metric | Value | Target | Status |
|---|---|---|---|
| Model | slim MLP (128->64), INT8 | -- | -- |
| Float32 F1 | 0.601 | -- | -- |
| INT8 F1 | 0.590 | -- | -- |
| F1 drop | 0.011 | < 3% | **PASS** |
| Size | **38.2 KB** | < 100 KB | **PASS** |
| Input dtype | int8, scale=0.041, zp=-21 | -- | Confirmed |
| Output dtype | int8, scale=0.004, zp=-128 | -- | Confirmed |

## Method

TFLite INT8 post-training quantization via `from_keras_model()` on TF 2.21.

**Critical fix required:** Warm-up call before conversion is mandatory to materialise
tf.Variable resources. Without it, the converter throws:
```
tensorflow/lite/kernels/read_variable.cc:67 variable != nullptr was not true.
Node number 16 (READ_VARIABLE) failed to invoke.
```

Working method:
```python
_ = model(np.zeros((1, 24), dtype='float32'), training=False)  # warm-up
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = rep_dataset  # 200 random training samples
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type = tf.int8
converter.inference_output_type = tf.int8
tflite_model = converter.convert()
```

## Previous attempts (2026-10-06)

Old `semg_model_int8.tflite` (123.7 KB) was trained on wrong labels (`stimulus` not
`restimulus`) and used a larger architecture. Superseded by `semg_v3_int8.tflite`.

## Log

- 2026-10-06: First INT8 attempt blocked by Keras3 MLIR bug (LLVM type conflict)
- 2026-10-18: Fixed via warm-up + from_keras_model(). 38.2 KB PASS. F1 drop 0.011 PASS.
