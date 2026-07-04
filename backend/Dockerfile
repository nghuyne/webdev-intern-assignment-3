# Stage 1: build the jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies separately from source so code changes don't re-download deps
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2: run the jar on a slim JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
