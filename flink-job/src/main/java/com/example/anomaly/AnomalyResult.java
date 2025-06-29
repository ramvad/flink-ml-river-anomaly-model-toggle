package com.example.anomaly;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnomalyResult {
    @JsonProperty("anomaly_score")
    private double anomalyScore;
    
    @JsonProperty("is_anomaly")
    private boolean isAnomaly;
    
    @JsonProperty("fraud_prediction")
    private boolean fraudPrediction;
    
    @JsonProperty("fraud_accuracy")
    private double fraudAccuracy;
    
    @JsonProperty("anomaly_model_used")
    private String anomalyModelUsed;
    
    // Default constructor
    public AnomalyResult() {}
    
    // Getters and setters
    public double getAnomalyScore() { return anomalyScore; }
    public void setAnomalyScore(double anomalyScore) { this.anomalyScore = anomalyScore; }
    
    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }
    
    public boolean isFraudPrediction() { return fraudPrediction; }
    public void setFraudPrediction(boolean fraudPrediction) { this.fraudPrediction = fraudPrediction; }
    
    public double getFraudAccuracy() { return fraudAccuracy; }
    public void setFraudAccuracy(double fraudAccuracy) { this.fraudAccuracy = fraudAccuracy; }
    
    public String getAnomalyModelUsed() { return anomalyModelUsed; }
    public void setAnomalyModelUsed(String anomalyModelUsed) { this.anomalyModelUsed = anomalyModelUsed; }
    
    @Override
    public String toString() {
        return "AnomalyResult{" +
                "anomalyScore=" + anomalyScore +
                ", isAnomaly=" + isAnomaly +
                ", fraudPrediction=" + fraudPrediction +
                ", fraudAccuracy=" + fraudAccuracy +
                ", anomalyModelUsed='" + anomalyModelUsed + '\'' +
                '}';
    }
}
