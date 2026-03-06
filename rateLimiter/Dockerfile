# Stage 1 -- Build
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

# Stage 2 -- Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 50051 9090
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
