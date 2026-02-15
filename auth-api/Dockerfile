FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Копируем pom.xml и загружаем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем приложение
COPY src ./src
RUN mvn clean package -DskipTests

# Финальный образ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный JAR файл
COPY --from=build /app/target/*.jar app.jar

# Настраиваем пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Открываем порт
EXPOSE 8081

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]