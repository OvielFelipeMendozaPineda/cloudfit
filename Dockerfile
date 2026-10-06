# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared-code/build.gradle.kts shared-code/
COPY accounts-service/build.gradle.kts accounts-service/
COPY wardrobe-service/build.gradle.kts wardrobe-service/
COPY styling-service/build.gradle.kts styling-service/
COPY billing-service/build.gradle.kts billing-service/
RUN chmod +x gradlew

ENV GRADLE_ARGS="--no-daemon -Dorg.gradle.jvmargs=-Xmx1g -Pkotlin.compiler.execution.strategy=in-process"

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew $GRADLE_ARGS dependencies > /dev/null || true

COPY shared-code/ shared-code/
COPY accounts-service/ accounts-service/
COPY wardrobe-service/ wardrobe-service/
COPY styling-service/ styling-service/
COPY billing-service/ billing-service/
COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew $GRADLE_ARGS installDist

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S cloudfit && adduser -S -G cloudfit cloudfit \
    && mkdir -p /data/images && chown -R cloudfit:cloudfit /data

WORKDIR /app
COPY --from=build /app/build/install/cloud-fit/ .

ENV JAVA_OPTS="-Xmx256m -XX:+UseSerialGC" \
    PORT=8080 \
    IMAGES_DIR=/data/images

USER cloudfit
EXPOSE 8080
VOLUME ["/data/images"]

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/health || exit 1

CMD ["./bin/cloud-fit"]
