
# Stage 1: Build the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven files first to improve build caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw -B dependency:go-offline

# Copy application source and build the JAR
COPY src ./src

RUN ./mvnw -B clean package -DskipTests

# Stage 2: Run the application with a smaller Java image
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create a non-root user
RUN useradd --system --uid 10001 --create-home appuser

COPY --from=build /app/target/*.jar app.jar

# Run as a non-root user
USER 10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
