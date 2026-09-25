# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 ryvenca && mkdir -p /data/media && chown -R ryvenca /data
COPY --from=build /app/target/ryvenca-backend-*.jar /app/app.jar
USER ryvenca
ENV SPRING_PROFILES_ACTIVE=prod \
    RYVENCA_STORAGE_DIR=/data/media
EXPOSE 8080
VOLUME ["/data"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
