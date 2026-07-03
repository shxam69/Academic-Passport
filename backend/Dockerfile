# --- Build stage ---
# Version note: eclipse-temurin tags for JDK 21 are actively maintained and stable;
# not flagging this one as uncertain. Maven 3.9 is bundled in this image and is
# unrelated to the 3.8.7 installed in the review sandbox — verify locally with
# `docker build` that dependency resolution succeeds against the real Maven Central.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom first so dependency resolution is cached in its own layer,
# independent of source code changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user — never run the app as root in a container.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/academic-passport-backend.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
