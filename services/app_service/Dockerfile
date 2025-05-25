# Stage 1: Builder
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy everything (respecting .dockerignore)
COPY . .

# Replace application-docker.properties with application.properties
RUN cp src/main/resources/application-docker.properties src/main/resources/application.properties

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runner
FROM openjdk:21-slim AS runner

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target ./target

# Expose port 8080
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["java", "-jar", "./target/app_service-1.0.0-RELEASE.jar"]