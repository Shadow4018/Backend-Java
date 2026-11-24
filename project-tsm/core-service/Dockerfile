# Lab 7: Dockerfile of core-service.
# Build from the REPOSITORY ROOT (the build needs the parent pom and the common module):
#   docker build -f core-service/Dockerfile -t tms-core-service .
#   docker run --rm -p 8081:8081 tms-core-service
# Connection settings are overridable through environment variables, e.g.
#   -e SPRING_DATASOURCE_URL=jdbc:mariadb://host.docker.internal:3307/... -e DB_PASSWORD=root

# ---- stage 1: build only this service (and the modules it needs: -am) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# poms first, so the dependency download is cached unless a pom changes
COPY pom.xml pom.xml
COPY common/pom.xml common/pom.xml
COPY core-service/pom.xml core-service/pom.xml
COPY glossary-service/pom.xml glossary-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY common/src common/src
COPY core-service/src core-service/src
RUN mvn -B -ntp -pl core-service -am -DskipTests package

# ---- stage 2: small runtime image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/core-service/target/core-service.jar app.jar
# the compose support (starts MariaDB/Keycloak) only makes sense on a developer machine
ENV SPRING_DOCKER_COMPOSE_ENABLED=false
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
