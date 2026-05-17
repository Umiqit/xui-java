# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/xui-bot-1.0-SNAPSHOT.jar app.jar
COPY .env .env
RUN mkdir -p logs
CMD ["java", "-jar", "app.jar"]
