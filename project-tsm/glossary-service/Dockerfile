# Lab 7: Dockerfile of glossary-service.
# Build from the REPOSITORY ROOT (the build needs the parent pom and the common module):
#   docker build -f glossary-service/Dockerfile -t tms-glossary-service .
#   docker run --rm -p 8082:8082 tms-glossary-service
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
COPY glossary-service/src glossary-service/src
RUN mvn -B -ntp -pl glossary-service -am -DskipTests package

# ---- stage 2: small runtime image ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/glossary-service/target/glossary-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
