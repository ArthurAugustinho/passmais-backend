# Multi-stage build for Spring Boot (Java 17)

# 1) Build stage
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy sources and build (skip tests for faster container builds)
COPY . .
RUN mvn -B -DskipTests clean package

# 2) Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the fat jar produced by Spring Boot plugin
COPY --from=build /app/target/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

# Optional JVM opts via JAVA_OPTS
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

EXPOSE 8080