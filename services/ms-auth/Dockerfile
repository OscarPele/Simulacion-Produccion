# syntax=docker/dockerfile:1.7

######### STAGE 1: BUILD #########
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src

# POMs
COPY pom.xml pom.xml
COPY services/shared.security/pom.xml services/shared.security/pom.xml
COPY services/ms-auth/pom.xml       services/ms-auth/pom.xml

# Código
COPY services/shared.security/src services/shared.security/src
COPY services/ms-auth/src          services/ms-auth/src

# 1) Instala shared.security en el repo local del contenedor
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests -f services/shared.security/pom.xml install

# 2) Empaqueta ms-auth (ya resolverá shared.security desde ~/.m2)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -DskipTests -f services/ms-auth/pom.xml package

######### STAGE 2: RUNTIME #########
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/services/ms-auth/target/*.jar /app/ms-auth.jar
USER 1001
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/ms-auth.jar"]