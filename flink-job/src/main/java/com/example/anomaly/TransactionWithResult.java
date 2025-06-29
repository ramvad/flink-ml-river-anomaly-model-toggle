package com.example.anomaly;

public class TransactionWithResult {
    private Transaction transaction;
    private AnomalyResult result;
    
    public TransactionWithResult() {}
    
    public TransactionWithResult(Transaction transaction, AnomalyResult result) {
        this.transaction = transaction;
        this.result = result;
    }
    
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    
    public AnomalyResult getResult() { return result; }
    public void setResult(AnomalyResult result) { this.result = result; }
    
    @Override
    public String toString() {
        return "TransactionWithResult{" +
                "transaction=" + transaction +
                ", result=" + result +
                '}';
    }
}
