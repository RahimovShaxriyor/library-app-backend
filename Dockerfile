# --- ЭТАП 1: СБОРКА (BUILD STAGE) ---
# Используем образ с Maven и Java 17 для сборки проекта
FROM maven:3.9-eclipse-temurin-17 AS builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем сначала pom.xml, чтобы кэшировать зависимости
COPY pom.xml .

# Копируем все дочерние модули
COPY auth ./auth
COPY order ./order
COPY gateway ./gateway
COPY registr ./registr
COPY payment ./payment

# Запускаем сборку всего проекта. Maven будет использовать кэш из /root/.m2
RUN mvn clean package -DskipTests

# --- ЭТАП 2: ЗАПУСК (RUN STAGE) ---
# Используем легкий образ только с Java Runtime для запуска
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Определяем, какой JAR-файл нужно будет скопировать
ARG MODULE_NAME
COPY --from=builder /app/${MODULE_NAME}/target/*.jar app.jar

# Указываем команду для запуска приложения
ENTRYPOINT ["java","-jar","/app/app.jar"]

