"""
train_cnn_v2.py  -- sEMG gesture classifier: MLP + properly-reshaped 1D-CNN
Ian Kiarie | FYP | JKUAT ECE 2026

Key fix over v1: the 24-element feature vector encodes 6 feature types x 4 channels.
- v1 treated it as a 24-step sequence with Conv1D -> no physically meaningful
  local structure; stuck at ~50% val accuracy even while RF achieved F1=0.70.
- v2 trains:
    A) MLP on flat 24-vector  (standard for hand-crafted feature vectors)
    B) 1D-CNN with reshape to (6 feature types, 4 channels) -- each kernel
       spans the same feature type across all 4 channels, which IS meaningful.
  Best model is quantized to INT8.
"""
import os, time
import numpy as np

BASE   = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA   = os.path.join(BASE, 'data', 'processed')
MODELS = os.path.join(BASE, 'models')
os.makedirs(MODELS, exist_ok=True)

print("Loading data...")
X = np.load(os.path.join(DATA, 'X_balanced.npy')).astype('float32')
y = np.load(os.path.join(DATA, 'y_balanced.npy')).astype('int32')
print(f"  X:{X.shape}  y:{y.shape}")

mean = X.mean(0); std = X.std(0)+1e-8
X_norm = (X-mean)/std
np.save(os.path.join(MODELS,'feature_mean.npy'), mean)
np.save(os.path.join(MODELS,'feature_std.npy'), std)

from sklearn.model_selection import train_test_split
from sklearn.metrics import f1_score, classification_report, confusion_matrix
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis
from sklearn.svm import SVC
from sklearn.ensemble import RandomForestClassifier
import joblib

X_tr, X_tmp, y_tr, y_tmp = train_test_split(X_norm,y,test_size=0.30,random_state=42,stratify=y)
X_val, X_te, y_val, y_te = train_test_split(X_tmp,y_tmp,test_size=0.50,random_state=42,stratify=y_tmp)
print(f"  Train:{X_tr.shape}  Val:{X_val.shape}  Test:{X_te.shape}")

print("\n--- Baselines ---")
bl = {'LDA':LinearDiscriminantAnalysis(),'SVM':SVC(kernel='rbf',C=10,gamma='scale',random_state=42),'RF':RandomForestClassifier(200,random_state=42,n_jobs=-1)}
bl_f1 = {}
for n,c in bl.items():
    t0=time.time(); c.fit(X_tr,y_tr)
    f1=f1_score(y_te,c.predict(X_te),average='macro')
    bl_f1[n]=f1; joblib.dump(c,os.path.join(MODELS,f'baseline_{n.lower()}.pkl'))
    print(f"  {n}: F1={f1:.4f} ({time.time()-t0:.1f}s)")

import tensorflow as tf
from tensorflow import keras, keras as K
from tensorflow.keras import layers
print(f"\nTF {tf.__version__}")
NC=8

def mlp():
    i=K.Input((24,),name='features')
    x=layers.Dense(128,activation='relu')(i)
    x=layers.BatchNormalization()(x)
    x=layers.Dropout(0.3)(x)
    x=layers.Dense(256,activation='relu')(x)
    x=layers.BatchNormalization()(x)
    x=layers.Dropout(0.3)(x)
    x=layers.Dense(128,activation='relu')(x)
    x=layers.Dropout(0.2)(x)
    o=layers.Dense(NC,activation='softmax',name='gesture')(x)
    return K.Model(i,o,name='semg_mlp')

def cnn():
    # Reshape: (24,) -> (6 feature types, 4 channels)
    # Each Conv1D kernel span = one feature type across all 4 channels
    i=K.Input((24,),name='features')
    x=layers.Reshape((6,4))(i)
    x=layers.Conv1D(32,3,padding='same',activation='relu')(x)
    x=layers.BatchNormalization()(x)
    x=layers.Conv1D(64,3,padding='same',activation='relu')(x)
    x=layers.BatchNormalization()(x)
    x=layers.GlobalAveragePooling1D()(x)
    x=layers.Dense(128,activation='relu')(x)
    x=layers.Dropout(0.3)(x)
    o=layers.Dense(NC,activation='softmax',name='gesture')(x)
    return K.Model(i,o,name='semg_1dcnn')

def fit(m,name,ep=150):
    m.compile(optimizer=K.optimizers.Adam(1e-3),loss='sparse_categorical_crossentropy',metrics=['accuracy'])
    cb=[K.callbacks.EarlyStopping(patience=15,restore_best_weights=True,verbose=1),
        K.callbacks.ReduceLROnPlateau(factor=0.5,patience=7,min_lr=1e-5,verbose=0),
        K.callbacks.ModelCheckpoint(os.path.join(MODELS,f'{name}_best.keras'),save_best_only=True,verbose=0)]
    t0=time.time()
    m.fit(X_tr,y_tr,validation_data=(X_val,y_val),epochs=ep,batch_size=64,callbacks=cb,verbose=1)
    yp=np.argmax(m.predict(X_te,verbose=0),1)
    f1=f1_score(y_te,yp,average='macro'); acc=np.mean(yp==y_te)
    print(f"\n{name}  F1={f1:.4f}  Acc={acc:.4f}  ({time.time()-t0:.0f}s)")
    m.save(os.path.join(MODELS,f'{name}_float32.keras'))
    return m,f1,acc

print("\n--- Model A: MLP ---")
m_mlp,f_mlp,a_mlp = fit(mlp(),'semg_mlp')

print("\n--- Model B: 1D-CNN (6x4 reshape) ---")
m_cnn,f_cnn,a_cnn = fit(cnn(),'semg_1dcnn')

best,bn,bf = (m_mlp,'semg_mlp',f_mlp) if f_mlp>=f_cnn else (m_cnn,'semg_1dcnn',f_cnn)
print(f"\nBest: {bn}  F1={bf:.4f}  ({'PASS' if bf>=0.85 else 'FAIL'} vs target 0.85)")

print("\n--- INT8 ---")
def rep():
    idx=np.random.choice(len(X_tr),200,replace=False)
    for i in idx: yield [X_tr[i:i+1]]

conv=tf.lite.TFLiteConverter.from_keras_model(best)
conv.optimizations=[tf.lite.Optimize.DEFAULT]
conv.representative_dataset=rep
conv.target_spec.supported_ops=[tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
conv.inference_input_type=tf.int8; conv.inference_output_type=tf.int8
tfl=conv.convert()
tp=os.path.join(MODELS,'semg_model_int8.tflite')
open(tp,'wb').write(tfl)
kb=os.path.getsize(tp)/1024
print(f"INT8 {kb:.1f}KB  ({'PASS' if kb<100 else 'FAIL'})")

interp=tf.lite.Interpreter(model_path=tp); interp.allocate_tensors()
id_=interp.get_input_details()[0]; od_=interp.get_output_details()[0]
isc,izp=id_['quantization']; osc,ozp=od_['quantization']
n,ok=min(500,len(X_te)),0
for i in range(n):
    xq=np.round(X_te[i]/isc+izp).astype('int8')[np.newaxis]
    interp.set_tensor(id_['index'],xq); interp.invoke()
    oq=interp.get_tensor(od_['index'])[0]
    ok+=(np.argmax((oq.astype('float32')-ozp)*osc)==y_te[i])
i8a=ok/n
print(f"INT8 acc={i8a:.4f}  drop={bf-i8a:.4f}")

yp=np.argmax(best.predict(X_te,verbose=0),1)
nm=['Rest','Open','Power','Pinch','Point','WristFlex','WristExt','ThumbsUp']
print("\nReport:"); print(classification_report(y_te,yp,target_names=nm))
np.save(os.path.join(MODELS,'confusion_matrix.npy'),confusion_matrix(y_te,yp))

print("\n"+"="*55)
print(f"  MLP   F1={f_mlp:.4f}  Acc={a_mlp:.4f}")
print(f"  CNN   F1={f_cnn:.4f}  Acc={a_cnn:.4f}")
for k,v in bl_f1.items(): print(f"  {k}    F1={v:.4f}")
print(f"  Best: {bn}  F1={bf:.4f}")
print(f"  INT8: {kb:.1f}KB  acc={i8a:.4f}")
print(f"  F1>=0.85: {'PASS' if bf>=0.85 else 'FAIL'}  <100KB: {'PASS' if kb<100 else 'FAIL'}")
print("="*55)
