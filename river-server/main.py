from fastapi import FastAPI, Request
from pydantic import BaseModel
from river import anomaly, linear_model, preprocessing, metrics
import uvicorn

app = FastAPI()

# Available anomaly models
available_anomaly_models = {
    "halfspacetrees": anomaly.HalfSpaceTrees(seed=42),
    "isolationforest": anomaly.IsolationForest(seed=42),
    "ensemble": [
        anomaly.HalfSpaceTrees(seed=42),
        anomaly.IsolationForest(seed=42)
    ]
}

# Default model setup
active_anomaly_model_name = "halfspacetrees"
anomaly_pipeline = preprocessing.MinMaxScaler() | available_anomaly_models[active_anomaly_model_name]

# Fraud classification pipeline
fraud_model = preprocessing.StandardScaler() | linear_model.LogisticRegression()
fraud_metric = metrics.Accuracy()

class Transaction(BaseModel):
    account_id: str
    timestamp: str
    amount: float
    merchant: str
    location: str
    is_fraud: int = None  # Optional during inference

class ModelChoice(BaseModel):
    model_name: str  # Options: "halfspacetrees", "isolationforest", "ensemble"

@app.post("/detect")
def detect(transaction: Transaction):
    features = {
        "amount": transaction.amount
    }

    # --- Anomaly Detection ---
    global anomaly_pipeline
    if active_anomaly_model_name == "ensemble":
        scores = []
        for model in available_anomaly_models["ensemble"]:
            scaler = preprocessing.MinMaxScaler() | model
            score = scaler.score_one(features)
            scaler.learn_one(features)
            scores.append(score)
        anomaly_score = sum(scores) / len(scores)
    else:
        anomaly_score = anomaly_pipeline.score_one(features)
        anomaly_pipeline.learn_one(features)

    # --- Fraud Detection ---
    y_pred = fraud_model.predict_one(features)
    if transaction.is_fraud is not None:
        fraud_model.learn_one(features, transaction.is_fraud)
        fraud_metric.update(transaction.is_fraud, y_pred)

    return {
        "anomaly_score": anomaly_score,
        "is_anomaly": anomaly_score > 0.5,
        "fraud_prediction": y_pred,
        "fraud_accuracy": fraud_metric.get(),
        "anomaly_model_used": active_anomaly_model_name
    }

@app.post("/set-model")
def set_model(model_choice: ModelChoice):
    global active_anomaly_model_name, anomaly_pipeline
    name = model_choice.model_name.lower()
    if name not in available_anomaly_models:
        return {"error": f"Model {name} not supported"}
    active_anomaly_model_name = name
    if name != "ensemble":
        anomaly_pipeline = preprocessing.MinMaxScaler() | available_anomaly_models[name]
    return {"message": f"Switched to model: {name}"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
