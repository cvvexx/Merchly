# Этап 1: Сборка
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Копируем ВЕСЬ исходный код проекта разом
# (мусор отсечется благодаря .dockerignore)
COPY . .

# Собираем ИМЕННО этот сервис
RUN mvn clean package -pl product-service -am -Dmaven.test.skip=true

# Этап 2: Запуск
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

# Забираем собранный jar-файл из папки модуля
COPY --from=builder /app/product-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]