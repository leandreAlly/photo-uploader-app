# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache dumb-init \
    && addgroup -S app \
    && adduser -S -G app app

WORKDIR /app

COPY --from=build --chown=app:app /build/target/app.jar ./app.jar

ARG VERSION=local
ARG COMMIT=unknown
ARG BUILT_AT=unknown

ENV APP_VERSION=$VERSION \
    APP_COMMIT=$COMMIT \
    APP_BUILT_AT=$BUILT_AT \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health >/dev/null 2>&1 || exit 1

ENTRYPOINT ["dumb-init", "--", "java", "-jar", "/app/app.jar"]
