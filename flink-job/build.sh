#!/bin/bash

# Build script for Flink Anomaly Detection Job
# Usage: ./build.sh [clean|package|run]

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_NAME="flink-river-anomaly-1.0-SNAPSHOT.jar"
TARGET_JAR="$PROJECT_DIR/target/$JAR_NAME"

case "${1:-package}" in
    clean)
        echo "🧹 Cleaning project..."
        mvn clean
        ;;
    
    package)
        echo "📦 Building project..."
        mvn clean package -DskipTests
        
        if [ -f "$TARGET_JAR" ]; then
            echo "✅ Build successful: $TARGET_JAR"
            echo "📊 JAR size: $(du -h "$TARGET_JAR" | cut -f1)"
        else
            echo "❌ Build failed"
            exit 1
        fi
        ;;
    
    run)
        echo "🚀 Running Flink job locally..."
        
        # Build if needed
        if [ ! -f "$TARGET_JAR" ]; then
            echo "📦 JAR not found, building first..."
            mvn clean package -DskipTests
        fi
        
        # Check if River API is accessible
        RIVER_URL="${RIVER_API_URL:-http://localhost:8000}"
        echo "🔍 Checking River API at $RIVER_URL..."
        
        if curl -s "$RIVER_URL/docs" > /dev/null; then
            echo "✅ River API is accessible"
        else
            echo "⚠️  Warning: River API not accessible at $RIVER_URL"
            echo "   Make sure the River service is running:"
            echo "   cd ../river-server && python main.py"
        fi
        
        # Run the job
        java -Xmx1g \
             -Dlog4j.logger.com.example.anomaly=DEBUG \
             -jar "$TARGET_JAR" \
             --river-api-url "${RIVER_API_URL:-http://localhost:8000}" \
             --data-path "${DATA_PATH:-../data/transactions.csv}" \
             --delay-ms "${DELAY_MS:-2000}"
        ;;
    
    test)
        echo "🧪 Running tests..."
        mvn test
        ;;
    
    docker)
        echo "🐳 Building Docker image..."
        docker build -t flink-anomaly-job .
        echo "✅ Docker image built: flink-anomaly-job"
        ;;
    
    *)
        echo "Usage: $0 [clean|package|run|test|docker]"
        echo ""
        echo "Commands:"
        echo "  clean   - Clean build artifacts"
        echo "  package - Build the JAR file (default)"
        echo "  run     - Build and run locally"
        echo "  test    - Run unit tests"
        echo "  docker  - Build Docker image"
        echo ""
        echo "Environment variables for 'run' command:"
        echo "  RIVER_API_URL - River API URL (default: http://localhost:8000)"
        echo "  DATA_PATH     - Path to CSV file (default: ../data/transactions.csv)"
        echo "  DELAY_MS      - Delay between records (default: 2000)"
        exit 1
        ;;
esac
