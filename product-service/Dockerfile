FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Слой зависимостей: перестраивается только при изменении pom-файлов, а не на каждую
# правку исходников. Блок одинаков во всех пяти Dockerfile, поэтому при сборке всего
# стека Maven-репозиторий скачивается один раз и переиспользуется остальными образами.
COPY pom.xml .
COPY config-server/pom.xml config-server/
COPY frontend-service/pom.xml frontend-service/
COPY order-service/pom.xml order-service/
COPY product-service/pom.xml product-service/
COPY review-service/pom.xml review-service/
COPY user-service/pom.xml user-service/
RUN mvn -B -ntp dependency:go-offline -Dmaven.test.skip=true

# Слой исходников. Тесты не гоняем: они требуют Docker (Testcontainers) и их
# прогоняет CI — образу нужен только jar.
COPY . .
RUN mvn -B -ntp package -pl product-service -am -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/product-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
