# Etapa 1: Download de dependências (cacheável)
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build
WORKDIR /workspace/app
COPY pom.xml .
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw clean package
RUN ./mvnw clean install -Dmaven.test.skip=true -Pnative

# Etapa final: Imagem runtime minimalista
FROM quay.io/quarkus/quarkus-distroless-image:1.0
WORKDIR /app
COPY --from=build /workspace/app/target/*-runner /app/application
COPY --from=build /workspace/app/src/main/resources/scripts/acquire_lock.lua /app/scripts/
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
