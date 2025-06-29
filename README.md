# Flink ML + River Real-Time Anomaly Detection

## Components

- **Synthetic transaction data** (`transactions.csv`)
- **Python FastAPI + River anomaly detection service** (Python/River ML)
- **Java Flink streaming application** (Java/Apache Flink)
- **Docker Compose setup** for orchestration

## Architecture

```text
CSV Data → Flink Job (Java) → River API (Python) → Anomaly Results
```

The system processes transaction data in real-time:

1. **Flink Job** reads transaction data and simulates streaming
2. **River API** performs ML-based anomaly detection using multiple models
3. **Results** are logged and anomalies are flagged in real-time

## Quick Start

```bash
# Build and run all services
docker-compose up --build

# The system will:
# 1. Start the River API service on port 8000
# 2. Launch the Flink job to process transactions
# 3. Stream results showing anomaly detection in real-time
```

## Project Structure

```text
├── data/                          # Transaction data
│   └── transactions.csv
├── river-server/                  # Python ML service
│   ├── Dockerfile
│   └── main.py
├── flink-job/                     # Java Flink application
│   ├── src/main/java/...
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml             # Orchestration
└── README.md
```

## Individual Components

- **River Server**: See `river-server/` for Python ML service details
- **Flink Job**: See `flink-job/README.md` for Java application details

