package com.example.anomaly;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.FileSource;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.core.fs.Path;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class FlinkAnomalyDetectionJob {
    
    private static final Logger LOG = LoggerFactory.getLogger(FlinkAnomalyDetectionJob.class);
    
    public static void main(String[] args) throws Exception {
        // Create execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Configure environment
        env.setParallelism(1);
        
        // Get configuration parameters
        String riverApiUrl = getParameter(args, "--river-api-url", "http://river-api:8000");
        String dataPath = getParameter(args, "--data-path", "/data/transactions.csv");
        long delayMs = Long.parseLong(getParameter(args, "--delay-ms", "1000"));
        
        LOG.info("Starting Flink Anomaly Detection Job");
        LOG.info("River API URL: {}", riverApiUrl);
        LOG.info("Data path: {}", dataPath);
        LOG.info("Delay between records: {}ms", delayMs);
        
        // Create data source that reads CSV and simulates streaming
        DataStream<Transaction> transactions = env
                .addSource(new CSVTransactionSource(dataPath, delayMs))
                .name("Transaction Source");
        
        // Call River API asynchronously for anomaly detection
        DataStream<TransactionWithResult> results = AsyncDataStream.unorderedWait(
                transactions,
                new RiverApiAsyncFunction(riverApiUrl),
                10000, // timeout
                TimeUnit.MILLISECONDS,
                10 // capacity
        ).name("Anomaly Detection");
        
        // Filter and log anomalies
        results
                .filter(result -> result.getResult() != null && result.getResult().isAnomaly())
                .map(new MapFunction<TransactionWithResult, String>() {
                    @Override
                    public String map(TransactionWithResult value) throws Exception {
                        Transaction tx = value.getTransaction();
                        AnomalyResult result = value.getResult();
                        return String.format("ANOMALY DETECTED! Account: %s, Amount: %.2f, Score: %.4f, Model: %s",
                                tx.getAccountId(), tx.getAmount(), result.getAnomalyScore(), result.getAnomalyModelUsed());
                    }
                })
                .print("Anomalies");
        
        // Log all results for monitoring
        results
                .map(new MapFunction<TransactionWithResult, String>() {
                    @Override
                    public String map(TransactionWithResult value) throws Exception {
                        if (value.getResult() == null) {
                            return "FAILED: " + value.getTransaction().toString();
                        }
                        Transaction tx = value.getTransaction();
                        AnomalyResult result = value.getResult();
                        return String.format("Processed: %s | Anomaly: %s (%.4f) | Fraud: %s | Model: %s",
                                tx.getAccountId(),
                                result.isAnomaly() ? "YES" : "NO",
                                result.getAnomalyScore(),
                                result.isFraudPrediction() ? "YES" : "NO",
                                result.getAnomalyModelUsed());
                    }
                })
                .print("All Results");
        
        // Execute the job
        env.execute("Flink River Anomaly Detection");
    }
    
    private static String getParameter(String[] args, String key, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
    
    // Custom source function to read CSV file and simulate streaming
    public static class CSVTransactionSource implements SourceFunction<Transaction> {
        private final String filePath;
        private final long delayMs;
        private volatile boolean isRunning = true;
        
        public CSVTransactionSource(String filePath, long delayMs) {
            this.filePath = filePath;
            this.delayMs = delayMs;
        }
        
        @Override
        public void run(SourceContext<Transaction> ctx) throws Exception {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                boolean isFirstLine = true;
                
                while (isRunning && (line = reader.readLine()) != null) {
                    // Skip header
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    
                    try {
                        Transaction transaction = parseCSVLine(line);
                        ctx.collect(transaction);
                        
                        // Simulate streaming delay
                        if (delayMs > 0) {
                            Thread.sleep(delayMs);
                        }
                    } catch (Exception e) {
                        LOG.warn("Error parsing line: {}", line, e);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error reading file: {}", filePath, e);
            }
        }
        
        @Override
        public void cancel() {
            isRunning = false;
        }
        
        private Transaction parseCSVLine(String line) {
            String[] fields = line.split(",");
            if (fields.length < 5) {
                throw new IllegalArgumentException("Invalid CSV line: " + line);
            }
            
            String accountId = fields[0].trim();
            String timestamp = fields[1].trim();
            double amount = Double.parseDouble(fields[2].trim());
            String merchant = fields[3].trim();
            String location = fields[4].trim();
            Integer isFraud = fields.length > 5 && !fields[5].trim().isEmpty() ? 
                    Integer.parseInt(fields[5].trim()) : null;
            
            return new Transaction(accountId, timestamp, amount, merchant, location, isFraud);
        }
    }
}
