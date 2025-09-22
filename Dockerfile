# syntax=docker/dockerfile:1

# 1) Build Spring Boot fat jar using Gradle wrapper
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Copy Gradle wrapper and project metadata first for better layer caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
COPY docs docs
COPY init-scripts init-scripts
COPY compose.yml compose.yml

# Ensure the wrapper is executable and build the application jar
RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon

# 2) Runtime image with JRE only
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Python and required pip packages for auxiliary scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && pip3 install --no-cache-dir google-genai pillow \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy the boot jar from the builder stage
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Default to prod profile; override with -e SPRING_PROFILES_ACTIVE=<profile>
ENV SPRING_PROFILES_ACTIVE=prod

# Expose application port (update if service runs on a different port)
EXPOSE 8080

# Run the Spring Boot application (pass --spring.profiles.active at runtime if needed)
ENTRYPOINT ["java","-jar","/app/app.jar"]
