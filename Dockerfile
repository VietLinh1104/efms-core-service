# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Define environment variables with default values
ENV SERVER_PORT=8082
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
