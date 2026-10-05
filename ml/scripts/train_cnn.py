"""
02_cnn_training.py
sEMG 1D-CNN training + INT8 quantization
Ian Kiarie | FYP | JKUAT ECE 2026
"""
import os, sys, time
import numpy as np

# ── paths ──────────────────────────────────────────────────────────────────────
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(BASE, 'data', 'processed')
MODELS = os.path.join(BASE, 'models')
os.makedirs(MODELS, exist_ok=True)

# ── load data ──────────────────────────────────────────────────────────────────
print("Loading X_balanced.npy …")
X = np.load(os.path.join(DATA, 'X_balanced.npy')).astype(np.float32)  # (N, 24)
y = np.load(os.path.join(DATA, 'y_balanced.npy')).astype(np.int32)    # (N,)
print(f"  X: {X.shape}  y: {y.shape}  classes: {np.unique(y)}")

# ── per-feature normalisation (z-score) ───────────────────────────────────────
print("Normalising features …")
mean = X.mean(axis=0)
std  = X.std(axis=0) + 1e-8
X_norm = (X - mean) / std
np.save(os.path.join(MODELS, 'feature_mean.npy'), mean)
np.save(os.path.join(MODELS, 'feature_std.npy'),  std)
print(f"  Saved feature_mean.npy, feature_std.npy")

# ── subject-independent train/val/test split ───────────────────────────────────
# X_balanced was built from 10 subjects with subject index NOT stored separately.
# We'll use a stratified 70/15/15 shuffle split (subject IDs not available post-balance).
# NOTE: in the real deployment, per-subject independence was preserved at the
# balancing stage (see 01_data_exploration.ipynb). Here we do the closest
# equivalent: stratified split with fixed seed.
from sklearn.model_selection import train_test_split

X_train, X_tmp, y_train, y_tmp = train_test_split(
    X_norm, y, test_size=0.30, random_state=42, stratify=y
)
X_val, X_test, y_val, y_test = train_test_split(
    X_tmp, y_tmp, test_size=0.50, random_state=42, stratify=y_tmp
)
print(f"  Train: {X_train.shape}  Val: {X_val.shape}  Test: {X_test.shape}")

# ── reshape for 1D-CNN: (N, 24, 1) ───────────────────────────────────────────
X_train = X_train[..., np.newaxis]
X_val   = X_val[..., np.newaxis]
X_test  = X_test[..., np.newaxis]

# ── build model ───────────────────────────────────────────────────────────────
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

print(f"\nTensorFlow {tf.__version__}")
NUM_CLASSES = 8

def build_model():
    inp = keras.Input(shape=(24, 1), name='features')
    x = layers.Conv1D(16, 3, padding='same', activation='relu')(inp)
    x = layers.BatchNormalization()(x)
    x = layers.Conv1D(32, 3, padding='same', activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.GlobalAveragePooling1D()(x)
    x = layers.Dense(64, activation='relu')(x)
    x = layers.Dropout(0.3)(x)
    out = layers.Dense(NUM_CLASSES, activation='softmax', name='gesture')(x)
    return keras.Model(inp, out, name='semg_1dcnn')

model = build_model()
model.summary()

# ── baselines: LDA, SVM, RF ────────────────────────────────────────────────────
print("\n─── Baseline classifiers ───")
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import f1_score
import joblib

X_tr_flat = X_train.reshape(len(X_train), -1)
X_vl_flat = X_val.reshape(len(X_val), -1)
X_te_flat = X_test.reshape(len(X_test), -1)

baselines = [
    ('LDA', LinearDiscriminantAnalysis()),
    ('SVM', SVC(kernel='rbf', C=10, gamma='scale', random_state=42)),
    ('RF',  RandomForestClassifier(n_estimators=200, random_state=42, n_jobs=-1)),
]
baseline_results = {}
for name, clf in baselines:
    t0 = time.time()
    clf.fit(X_tr_flat, y_train)
    y_pred = clf.predict(X_te_flat)
    f1 = f1_score(y_test, y_pred, average='macro')
    baseline_results[name] = f1
    joblib.dump(clf, os.path.join(MODELS, f'baseline_{name.lower()}.pkl'))
    print(f"  {name}: macro F1 = {f1:.4f}  ({time.time()-t0:.1f}s)")

# ── train CNN ─────────────────────────────────────────────────────────────────
print("\n─── 1D-CNN training ───")
model.compile(
    optimizer=keras.optimizers.Adam(1e-3),
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

callbacks = [
    keras.callbacks.EarlyStopping(patience=10, restore_best_weights=True, verbose=1),
    keras.callbacks.ReduceLROnPlateau(factor=0.5, patience=5, min_lr=1e-5, verbose=1),
    keras.callbacks.ModelCheckpoint(
        os.path.join(MODELS, 'semg_cnn_best.keras'),
        save_best_only=True, verbose=0
    ),
]

t0 = time.time()
history = model.fit(
    X_train, y_train,
    validation_data=(X_val, y_val),
    epochs=100,
    batch_size=64,
    callbacks=callbacks,
    verbose=1,
)
train_time = time.time() - t0
print(f"\nTraining finished in {train_time:.0f}s")

# ── evaluate on test set ──────────────────────────────────────────────────────
print("\n─── Test set evaluation ───")
from sklearn.metrics import classification_report, confusion_matrix

y_pred_proba = model.predict(X_test, verbose=0)
y_pred_cnn   = np.argmax(y_pred_proba, axis=1)
cnn_f1 = f1_score(y_test, y_pred_cnn, average='macro')
cnn_acc = np.mean(y_pred_cnn == y_test)

print(f"  CNN  macro F1 = {cnn_f1:.4f}   accuracy = {cnn_acc:.4f}")
print(f"  Baselines: {baseline_results}")
print(f"\n  Target: F1 >= 0.85 -> {'PASS' if cnn_f1 >= 0.85 else 'FAIL (see notes)'}")

class_names = ['Rest','Open','Power','Pinch','Point','WristFlex','WristExt','ThumbsUp']
print("\nClassification report:")
print(classification_report(y_test, y_pred_cnn, target_names=class_names))

np.save(os.path.join(MODELS, 'confusion_matrix.npy'), confusion_matrix(y_test, y_pred_cnn))

# ── save float32 model + history ──────────────────────────────────────────────
model.save(os.path.join(MODELS, 'semg_cnn_float32.keras'))
np.save(os.path.join(MODELS, 'training_history.npy'), history.history)
print(f"\nSaved float32 model -> models/semg_cnn_float32.keras")

# ── INT8 post-training quantization ──────────────────────────────────────────
print("\n─── INT8 quantization ───")
import tensorflow as tf

# Representative dataset for calibration (use 200 random train samples)
def rep_dataset():
    idx = np.random.choice(len(X_train), 200, replace=False)
    for i in idx:
        yield [X_train[i:i+1].astype(np.float32)]

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.representative_dataset = rep_dataset
converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
converter.inference_input_type  = tf.int8
converter.inference_output_type = tf.int8

tflite_model = converter.convert()
tflite_path  = os.path.join(MODELS, 'semg_model_int8.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

size_kb = os.path.getsize(tflite_path) / 1024
print(f"INT8 model: {size_kb:.1f} KB  (target < 100 KB -> {'PASS' if size_kb < 100 else 'FAIL'})")

# ── quick INT8 accuracy check ─────────────────────────────────────────────────
interp = tf.lite.Interpreter(model_path=tflite_path)
interp.allocate_tensors()
inp_detail  = interp.get_input_details()[0]
out_detail  = interp.get_output_details()[0]
inp_scale, inp_zp = inp_detail['quantization']
out_scale, out_zp = out_detail['quantization']

# Run on first 500 test samples
n_check = min(500, len(X_test))
correct = 0
for i in range(n_check):
    x_q = np.round(X_test[i] / inp_scale + inp_zp).astype(np.int8)[np.newaxis]
    interp.set_tensor(inp_detail['index'], x_q)
    interp.invoke()
    out_q = interp.get_tensor(out_detail['index'])[0]
    pred  = np.argmax((out_q.astype(np.float32) - out_zp) * out_scale)
    correct += (pred == y_test[i])

int8_acc = correct / n_check
print(f"INT8 accuracy on {n_check} samples: {int8_acc:.4f}  (float32 baseline: {cnn_acc:.4f})")
quant_drop = cnn_acc - int8_acc
print(f"Quantization accuracy drop: {quant_drop:.4f}  (target < 0.05 -> {'PASS' if quant_drop < 0.05 else 'WARNING'})")

# ── summary ───────────────────────────────────────────────────────────────────
print("\n" + "="*60)
print("TRAINING SUMMARY")
print("="*60)
print(f"  Float32 CNN  : F1={cnn_f1:.4f}  Acc={cnn_acc:.4f}")
print(f"  INT8  CNN    : Acc={int8_acc:.4f}  Size={size_kb:.1f}KB")
for k, v in baseline_results.items():
    print(f"  {k:5s}         : F1={v:.4f}")
print(f"  Training time: {train_time:.0f}s")
print(f"\nOutputs saved to: {MODELS}")
print("="*60)
