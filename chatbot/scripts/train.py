"""
Trains a multi-class intent classifier (TF-IDF + Logistic Regression)
on the medical dataset, evaluates it, and exports the model in a
JSON format that the Java side can load and use for inference
without any Python dependency at runtime.
"""
import json
import re
import math
from pathlib import Path
from collections import Counter

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.multiclass import OneVsRestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "data"
MODEL = ROOT / "model"
MODEL.mkdir(parents=True, exist_ok=True)

# ---------- 1. Load dataset ----------
with open(DATA / "training_set.json", encoding="utf-8") as f:
    flat = json.load(f)

texts = [it["text"] for it in flat]
labels = [it["label"] for it in flat]

print(f"Loaded {len(texts)} examples across {len(set(labels))} classes")

# ---------- 2. Split ----------
X_train, X_test, y_train, y_test = train_test_split(
    texts, labels, test_size=0.15, stratify=labels, random_state=42
)
print(f"Train: {len(X_train)}  | Test: {len(X_test)}")


# ---------- 3. Custom simple tokenizer (will be re-implemented in Java) ----------
TOKEN_RE = re.compile(r"[a-zA-ZÀ-ſ]+", re.UNICODE)


def tokenize(text: str):
    return [w.lower() for w in TOKEN_RE.findall(text or "")]


# ---------- 4. Vectorize (TF-IDF on uni+bigrams) ----------
vec = TfidfVectorizer(
    tokenizer=tokenize,
    token_pattern=None,
    ngram_range=(1, 2),
    min_df=2,
    sublinear_tf=True,
    norm="l2",
    lowercase=False,
)
Xtr = vec.fit_transform(X_train)
Xte = vec.transform(X_test)

print(f"Vocabulary size: {len(vec.vocabulary_)}")

# ---------- 5. Train classifier ----------
base = LogisticRegression(max_iter=2000, C=4.0, solver="liblinear")
clf = OneVsRestClassifier(base)
clf.fit(Xtr, y_train)

# ---------- 6. Evaluate ----------
y_pred = clf.predict(Xte)
acc = accuracy_score(y_test, y_pred)
print(f"\nAccuracy on test set: {acc:.4f}")

report = classification_report(y_test, y_pred, zero_division=0, output_dict=True)
print(classification_report(y_test, y_pred, zero_division=0))

# Confusion matrix as plain text (no matplotlib needed)
labels_sorted = sorted(set(y_train))
cm = confusion_matrix(y_test, y_pred, labels=labels_sorted)


# ---------- 7. Export model to a Java-friendly JSON ----------
# The Java side will:
#   - tokenize input the same way (regex word chars)
#   - build TF vector (count) → apply sublinear_tf (1 + log) → multiply by IDF → L2 normalize
#   - then for each class: dot product (vector × class_weights) + intercept → argmax
#
# We emit:
#   - tokenizer = "word_lower" (regex [a-zA-ZÀ-ſ]+)
#   - vocab = { ngram: index }   where ngram is "word" or "word1 word2"
#   - idf  = [N]                  (one float per ngram index)
#   - sublinear_tf = true
#   - norm = "l2"
#   - classes = [labels]
#   - weights = [n_classes][n_features]
#   - intercept = [n_classes]
#   - intents_meta = { tag: {response, product_types, urgency} } loaded from JSON

vocab = {term: int(idx) for term, idx in vec.vocabulary_.items()}
idf = vec.idf_.tolist()

# Stack the per-class binary classifiers' weights
weights = np.vstack([e.coef_[0] for e in clf.estimators_]).astype(np.float32).tolist()
intercept = np.array([e.intercept_[0] for e in clf.estimators_], dtype=np.float32).tolist()
classes = list(map(str, clf.classes_))

# Load intents to package responses
with open(DATA / "medical_intents.json", encoding="utf-8") as f:
    intents_full = json.load(f)["intents"]
meta = {
    it["tag"]: {
        "response": it["response"],
        "product_types": it["product_types"],
        "urgency": it["urgency"],
    }
    for it in intents_full
}

model_out = {
    "version": 1,
    "tokenizer": {"type": "word_lower", "regex": r"[a-zA-ZÀ-ſ]+"},
    "ngrams": {"min": 1, "max": 2},
    "sublinear_tf": True,
    "norm": "l2",
    "vocab": vocab,
    "idf": idf,
    "classes": classes,
    "weights": weights,
    "intercept": intercept,
    "intents_meta": meta,
    "metrics": {
        "accuracy": acc,
        "n_train": len(X_train),
        "n_test": len(X_test),
        "n_classes": len(classes),
        "n_features": len(vocab),
    },
}

out_path = MODEL / "medical_classifier.json"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(model_out, f, ensure_ascii=False)

# Also write a small summary report
with open(MODEL / "training_report.md", "w", encoding="utf-8") as f:
    f.write("# Training Report — Medical Intent Classifier\n\n")
    f.write(f"- **Total examples**: {len(texts)}\n")
    f.write(f"- **Train**: {len(X_train)} | **Test**: {len(X_test)}\n")
    f.write(f"- **Classes**: {len(classes)}\n")
    f.write(f"- **Vocabulary**: {len(vocab)} ngrams (uni + bi)\n")
    f.write(f"- **Architecture**: TF-IDF + Logistic Regression (one-vs-rest)\n\n")
    f.write(f"## Accuracy\n\n**{acc:.4f}** on held-out test set\n\n")
    f.write("## Per-class metrics (test)\n\n| Intent | Precision | Recall | F1 | Support |\n|---|---|---|---|---|\n")
    for cls in sorted(classes):
        if cls in report:
            r = report[cls]
            f.write(f"| {cls} | {r['precision']:.2f} | {r['recall']:.2f} | {r['f1-score']:.2f} | {int(r['support'])} |\n")
    f.write(f"\n## Notes\n- Tokenizer : regex `[a-zA-ZÀ-ſ]+` (lowercased)\n- N-grams : 1+2\n- `sublinear_tf=True` (TF replaced by 1+log(TF))\n- `norm=l2` (L2 row normalisation)\n- Solver : liblinear (one-vs-rest)\n")

# Also write confusion matrix as CSV
with open(MODEL / "confusion_matrix.csv", "w", encoding="utf-8") as f:
    f.write("," + ",".join(labels_sorted) + "\n")
    for i, label in enumerate(labels_sorted):
        f.write(label + "," + ",".join(map(str, cm[i].tolist())) + "\n")

size_mb = out_path.stat().st_size / 1024 / 1024
print(f"\nModel saved -> {out_path}  ({size_mb:.2f} MB)")
print(f"Report saved -> {MODEL / 'training_report.md'}")
print(f"Confusion matrix CSV -> {MODEL / 'confusion_matrix.csv'}")
