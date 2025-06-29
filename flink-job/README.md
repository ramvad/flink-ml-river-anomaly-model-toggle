# Flink Anomaly Detection Job

A Java-based Apache Flink streaming application that processes transaction data in real-time and performs anomaly detection by calling the River ML API service.

## Architecture Overview

```text
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   CSV Source    │───▶│   Flink Job      │───▶│   River API     │
│ (transactions)  │    │  (Java/Flink)    │    │ (Python/River)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   Console       │
                       │   Output        │
                       └─────────────────┘
```

### Core Components

1. **CSVTransactionSource**: Custom Flink source that reads CSV data and simulates streaming
2. **RiverApiAsyncFunction**: Asynchronous function that calls the River ML API
3. **Transaction Models**: POJOs for data serialization/deserialization
4. **Main Job**: Orchestrates the streaming pipeline

## Technical Architecture

### Data Flow

1. **Data Ingestion**: CSV file is read line-by-line with configurable delays to simulate real-time streaming
2. **Async Processing**: Each transaction is sent to the River API using HTTP async calls
3. **Result Processing**: API responses are parsed and anomalies are filtered and logged
4. **Output**: Both anomalies and all results are printed to console

### Key Features

- **Asynchronous API Calls**: Non-blocking HTTP requests to the River service
- **Fault Tolerance**: Graceful handling of API failures and timeouts
- **Configurable Parameters**: Command-line arguments for customization
- **Real-time Simulation**: Configurable delays between record processing
- **Multiple Output Streams**: Separate streams for anomalies and all results

## Project Structure

```text
flink-job/
├── src/main/java/com/example/anomaly/
│   ├── FlinkAnomalyDetectionJob.java     # Main application entry point
│   ├── RiverApiAsyncFunction.java        # Async API client
│   ├── Transaction.java                  # Transaction data model
│   ├── AnomalyResult.java               # API response model
│   └── TransactionWithResult.java       # Combined result model
├── Dockerfile                           # Container configuration
├── pom.xml                             # Maven dependencies
└── README.md                           # This file
```

## Dependencies

### Core Dependencies

- **Apache Flink 1.18.0**: Stream processing framework
- **Jackson**: JSON serialization/deserialization
- **Apache HttpClient 5**: Async HTTP client for API calls
- **Log4j 2**: Logging framework

### Maven Configuration

The project uses Maven Shade plugin to create a fat JAR with all dependencies included.

## Running the Application

### Using Docker Compose (Recommended)

```bash
# From the root project directory
docker-compose up --build

# This will:
# 1. Build the Flink job Docker image
# 2. Start the River API service
# 3. Run the Flink job with default parameters
```

### Local Development

#### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Running River API service (see `../river-server/`)

#### Build and Run

```bash
# Build the project
mvn clean package

# Run with default parameters
java -jar target/flink-river-anomaly-1.0-SNAPSHOT.jar

# Run with custom parameters
java -jar target/flink-river-anomaly-1.0-SNAPSHOT.jar \
  --river-api-url http://localhost:8000 \
  --data-path ../data/transactions.csv \
  --delay-ms 1000
```

### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--river-api-url` | `http://river-api:8000` | Base URL for the River API service |
| `--data-path` | `/data/transactions.csv` | Path to the transaction CSV file |
| `--delay-ms` | `1000` | Delay between processing records (milliseconds) |

## Expected CSV Format

The application expects CSV files with the following format:

```csv
account_id,timestamp,amount,merchant,location,is_fraud
ACC001,2024-01-01T10:00:00,150.00,Amazon,Seattle,0
ACC002,2024-01-01T10:01:00,5000.00,ATM,Unknown,1
```

### Field Descriptions

- **account_id**: Unique account identifier
- **timestamp**: Transaction timestamp (ISO format)
- **amount**: Transaction amount (decimal)
- **merchant**: Merchant name
- **location**: Transaction location
- **is_fraud**: Fraud label (0/1, optional for inference)

## Output Examples

### Anomaly Detection Output

```text
Anomalies> ANOMALY DETECTED! Account: ACC002, Amount: 5000.00, Score: 0.8567, Model: halfspacetrees

All Results> Processed: ACC001 | Anomaly: NO (0.2341) | Fraud: NO | Model: halfspacetrees
All Results> Processed: ACC002 | Anomaly: YES (0.8567) | Fraud: YES | Model: halfspacetrees
```

## API Integration

### River API Endpoints Used

- **POST /detect**: Submit transaction for anomaly detection
- **POST /set-model**: Change the active ML model (future enhancement)

### Request Format

```json
{
  "account_id": "ACC001",
  "timestamp": "2024-01-01T10:00:00",
  "amount": 150.00,
  "merchant": "Amazon",
  "location": "Seattle",
  "is_fraud": 0
}
```

### Response Format

```json
{
  "anomaly_score": 0.2341,
  "is_anomaly": false,
  "fraud_prediction": false,
  "fraud_accuracy": 0.85,
  "anomaly_model_used": "halfspacetrees"
}
```

## Performance Considerations

### Async Processing

The application uses Flink's `AsyncDataStream` for non-blocking API calls:

- **Capacity**: 10 concurrent requests
- **Timeout**: 10 seconds per request
- **Backpressure**: Automatic handling when API is slow

### Resource Configuration

- **Memory**: 1GB heap size (configurable via JAVA_OPTS)
- **Parallelism**: Default 1 (can be increased for higher throughput)
- **Checkpointing**: Not enabled (can be added for production)

## Error Handling

### API Failures

- **Connection Errors**: Logged as warnings, processing continues
- **Timeouts**: 10-second timeout with fallback to null results
- **Parse Errors**: Invalid JSON responses are logged and skipped

### Data Quality

- **Invalid CSV**: Malformed lines are logged and skipped
- **Missing Fields**: Required fields are validated during parsing

## Monitoring and Logging

### Log Levels

- **INFO**: Job startup, configuration, and major events
- **DEBUG**: Individual transaction processing details
- **WARN**: API failures and data quality issues
- **ERROR**: Critical failures that may stop processing

### Metrics (Future Enhancement)

Potential metrics to implement:

- Transactions processed per second
- API response times
- Anomaly detection rate
- Error rates by type

## Future Enhancements

### Short-term Improvements

1. **Enhanced Error Handling**
   - Retry mechanisms for failed API calls
   - Circuit breaker pattern for API resilience
   - Dead letter queue for failed transactions

2. **Configuration Management**
   - External configuration files (application.yml)
   - Environment-specific configurations
   - Runtime parameter updates

3. **Data Connectors**
   - Kafka source connector for real streaming data
   - Database sink for storing results
   - File sink for audit trails

### Medium-term Features

1. **State Management**
   - Flink checkpointing for fault tolerance
   - State backends (RocksDB, HDFS)
   - Savepoint management for upgrades

2. **Monitoring & Observability**
   - Prometheus metrics integration
   - Custom Flink metrics
   - Health check endpoints
   - Grafana dashboards

3. **Performance Optimization**
   - Connection pooling for HTTP client
   - Batch API calls for better throughput
   - Parallelism tuning and optimization

### Long-term Enhancements

1. **Advanced Stream Processing**
   - Windowed aggregations for pattern detection
   - Complex event processing (CEP)
   - Multi-stream joins (user profiles, historical data)

2. **ML Model Integration**
   - Direct Flink ML integration (alternative to API calls)
   - Model versioning and A/B testing
   - Online learning feedback loops

3. **Enterprise Features**
   - Schema registry integration
   - Data lineage tracking
   - Compliance and audit logging
   - Multi-tenant support

### Deployment Enhancements

1. **Kubernetes Deployment**
   - Helm charts for easy deployment
   - Horizontal pod autoscaling
   - Service mesh integration (Istio)

2. **CI/CD Pipeline**
   - Automated testing (unit, integration)
   - Code quality gates (SonarQube)
   - Automated deployment to staging/production

3. **Data Platform Integration**
   - Apache Iceberg for data lake integration
   - Delta Lake for transaction logs
   - DataHub for metadata management

## Development Guidelines

### Code Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Add comprehensive JavaDoc for public methods
- Maintain consistent indentation (4 spaces)

### Testing Strategy

- Unit tests for data models and utility functions
- Integration tests with test containers
- End-to-end tests with embedded Flink clusters
- Performance tests with JMH

### Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit a pull request with detailed description

## Troubleshooting

### Common Issues

#### "Connection refused" to River API

```bash
# Check if River API is running
curl http://localhost:8000/docs

# Verify network connectivity in Docker
docker-compose logs river-api
```

#### Out of Memory Errors

```bash
# Increase heap size
export JAVA_OPTS="-Xmx2g"
java $JAVA_OPTS -jar target/flink-river-anomaly-1.0-SNAPSHOT.jar
```

#### CSV Parsing Errors

```bash
# Check CSV format
head -5 ../data/transactions.csv

# Verify file encoding (should be UTF-8)
file ../data/transactions.csv
```

### Debug Mode

Enable debug logging by adding to JVM args:

```bash
java -Dlog4j.logger.com.example.anomaly=DEBUG -jar target/flink-river-anomaly-1.0-SNAPSHOT.jar
```

## License

This project is part of the Flink ML + River Anomaly Detection system. See the main project for license details.

## Support

For issues and questions:

1. Check the troubleshooting section above
2. Review Flink documentation: <https://flink.apache.org/>
3. Check River ML documentation for API details
4. Open an issue in the project repository
