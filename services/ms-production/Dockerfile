# syntax=docker/dockerfile:1.7

######### STAGE 1: BUILD #########
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src

# POMs
COPY pom.xml pom.xml
COPY services/shared.security/pom.xml  services/shared.security/pom.xml
COPY services/ms-production/pom.xml    services/ms-production/pom.xml

# Código
COPY services/shared.security/src  services/shared.security/src
COPY services/ms-production/src    services/ms-production/src

# 1) Instala shared.security en repo local del contenedor
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests -f services/shared.security/pom.xml install

# 2) Empaqueta ms-production
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests -f services/ms-production/pom.xml package

######### STAGE 2: RUNTIME #########
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/services/ms-production/target/*.jar /app/ms-production.jar
USER 1001
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/ms-production.jar","--spring.profiles.active=prod"]
