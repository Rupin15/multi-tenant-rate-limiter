FROM maven:3-eclipse-temurin-25 AS build

ARG MODULE
WORKDIR /workspace

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY ${MODULE}/src ${MODULE}/src

RUN mvn -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:25-jre

WORKDIR /app
ARG MODULE
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
