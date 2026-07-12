# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-25 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven POM file to download dependencies
COPY pom.xml .

# Download dependencies to leverage Docker cache
RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Build the Spring Boot application
# -DskipTests is used to skip running tests during the build
RUN mvn clean install -DskipTests

# Stage 2: Create the final runtime image
FROM eclipse-temurin:25-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the builder stage
# Assuming the JAR name is swoopdServer-0.0.1-SNAPSHOT.jar based on typical Spring Boot Maven projects
COPY --from=builder /app/target/swoopdServer-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that Spring Boot runs on (default is 8080)
EXPOSE 8080

# Define the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
