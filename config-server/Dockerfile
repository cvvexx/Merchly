FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY config-server/pom.xml config-server/
COPY frontend-service/pom.xml frontend-service/
COPY order-service/pom.xml order-service/
COPY product-service/pom.xml product-service/
COPY review-service/pom.xml review-service/
COPY user-service/pom.xml user-service/
COPY gateway-service/pom.xml gateway-service/
RUN mvn -B -ntp dependency:go-offline -Dmaven.test.skip=true

COPY . .
RUN mvn -B -ntp package -pl config-server -am -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV CONFIG_SERVER_PROFILE=git

COPY --from=builder /app/config-server/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
