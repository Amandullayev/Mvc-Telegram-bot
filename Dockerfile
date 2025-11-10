FROM ubuntu:latest
LABEL authors="alikhan"

ENTRYPOINT ["top", "-b"]

FROM openjdk:21-jdk-slim

WORKDIR /app

# Maven va pom.xml ni nusxalaymiz
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Dependency'larni yuklab olamiz (tezlik uchun)
RUN ./mvnw dependency:go-offline

# Barcha kodlarni nusxalaymiz
COPY src src

# JAR faylni yaratamiz
RUN ./mvnw clean package -DskipTests

# JAR faylni ishga tushiramiz
CMD ["java", "-jar", "target/*.jar"]