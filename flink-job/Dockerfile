FROM openjdk:11-jre-slim

WORKDIR /app

# Install Maven for building
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create runtime image
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built JAR
COPY --from=0 /app/target/flink-river-anomaly-1.0-SNAPSHOT.jar app.jar

# Expose Flink ports
EXPOSE 8080 8081

CMD ["java", "-jar", "app.jar"]
