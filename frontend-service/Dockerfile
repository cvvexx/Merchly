FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY . .

RUN mvn clean package -pl frontend-service -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/frontend-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]