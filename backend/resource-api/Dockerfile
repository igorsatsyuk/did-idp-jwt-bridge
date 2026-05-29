FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml ./pom.xml
COPY common ./common
COPY identity-service ./identity-service
COPY auth-bridge-service ./auth-bridge-service
COPY resource-api ./resource-api

RUN mvn -pl resource-api -am -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/resource-api/target/resource-api-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
