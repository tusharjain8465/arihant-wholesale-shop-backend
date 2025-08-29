# Step 1: Use a JDK image to build the app
FROM openjdk:17-jdk-slim as builder
WORKDIR /app

# Copy Maven/Gradle files first (for caching dependencies)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# Step 2: Run the built app
FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Expose the port for Render
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]

