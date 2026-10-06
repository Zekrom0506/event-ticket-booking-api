# ---- Stage 1: build the jar with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy pom.xml first so the dependencies are cached between builds
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
# tests are run by the CI pipeline before this, so skip them here
RUN mvn -q package -DskipTests

# ---- Stage 2: small image that only runs the jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/ticket-booking-api-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
