# ---- Build Stage ----
FROM gradle:8.13-jdk21-alpine AS build

WORKDIR /build

# Copy Gradle build scripts first (for caching dependencies)
COPY build.gradle settings.gradle ./

# Pre-download dependencies
RUN gradle build --no-daemon || true

# Now copy full project
COPY src ./src

# Build the fat JAR
RUN gradle bootJar --no-daemon

# Use -alpine because its optimized for space and it takes less memory
FROM amazoncorretto:21-alpine

# set working directory
WORKDIR /app

# Copy the built JAR from previous stage
COPY --from=build /build/build/libs/*.jar voltpay-writer.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "voltpay-writer.jar"]