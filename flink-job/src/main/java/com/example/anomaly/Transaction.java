package com.example.anomaly;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
    @JsonProperty("account_id")
    private String accountId;
    
    @JsonProperty("timestamp")
    private String timestamp;
    
    @JsonProperty("amount")
    private double amount;
    
    @JsonProperty("merchant")
    private String merchant;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("is_fraud")
    private Integer isFraud;
    
    // Default constructor
    public Transaction() {}
    
    // Constructor
    public Transaction(String accountId, String timestamp, double amount, 
                      String merchant, String location, Integer isFraud) {
        this.accountId = accountId;
        this.timestamp = timestamp;
        this.amount = amount;
        this.merchant = merchant;
        this.location = location;
        this.isFraud = isFraud;
    }
    
    // Getters and setters
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Integer getIsFraud() { return isFraud; }
    public void setIsFraud(Integer isFraud) { this.isFraud = isFraud; }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "accountId='" + accountId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", amount=" + amount +
                ", merchant='" + merchant + '\'' +
                ", location='" + location + '\'' +
                ", isFraud=" + isFraud +
                '}';
    }
}
