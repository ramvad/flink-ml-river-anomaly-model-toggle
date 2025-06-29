package com.example.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.RichAsyncFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class RiverApiAsyncFunction extends RichAsyncFunction<Transaction, TransactionWithResult> {
    
    private static final Logger LOG = LoggerFactory.getLogger(RiverApiAsyncFunction.class);
    
    private final String apiUrl;
    private transient CloseableHttpAsyncClient httpClient;
    private transient ObjectMapper objectMapper;
    
    public RiverApiAsyncFunction(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
        objectMapper = new ObjectMapper();
        LOG.info("River API client initialized with URL: {}", apiUrl);
    }
    
    @Override
    public void close() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
        super.close();
    }
    
    @Override
    public void asyncInvoke(Transaction transaction, ResultFuture<TransactionWithResult> resultFuture) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(transaction);
            
            SimpleHttpRequest request = SimpleRequestBuilder.post(apiUrl + "/detect")
                    .setBody(jsonPayload, ContentType.APPLICATION_JSON)
                    .build();
            
            CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();
            
            httpClient.execute(request, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse response) {
                    future.complete(response);
                }
                
                @Override
                public void failed(Exception ex) {
                    future.completeExceptionally(ex);
                }
                
                @Override
                public void cancelled() {
                    future.cancel(true);
                }
            });
            
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    LOG.error("Error calling River API", throwable);
                    // Return transaction with null result on error
                    resultFuture.complete(Collections.singleton(new TransactionWithResult(transaction, null)));
                } else {
                    try {
                        String responseBody = response.getBodyText();
                        AnomalyResult result = objectMapper.readValue(responseBody, AnomalyResult.class);
                        resultFuture.complete(Collections.singleton(new TransactionWithResult(transaction, result)));
                        LOG.debug("Successfully processed transaction: {} with result: {}", 
                                transaction.getAccountId(), result.isAnomaly());
                    } catch (Exception e) {
                        LOG.error("Error parsing River API response", e);
                        resultFuture.complete(Collections.singleton(new TransactionWithResult(transaction, null)));
                    }
                }
            });
            
        } catch (Exception e) {
            LOG.error("Error preparing request to River API", e);
            resultFuture.complete(Collections.singleton(new TransactionWithResult(transaction, null)));
        }
    }
    
    @Override
    public void timeout(Transaction input, ResultFuture<TransactionWithResult> resultFuture) {
        LOG.warn("Timeout calling River API for transaction: {}", input.getAccountId());
        resultFuture.complete(Collections.singleton(new TransactionWithResult(input, null)));
    }
}
