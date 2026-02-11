# syntax=docker/dockerfile:1.7

#############################################
# 1) Build stage
#############################################
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

RUN apk add --no-cache nodejs npm

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY package.json package-lock.json* tailwind.config.js* ./

RUN chmod +x ./gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies

COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar -x test

#############################################
# 2) Runtime stage
#############################################
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S worker -g 1000 \
    && adduser -S worker -u 1000 -G worker -D \
    && chown -R worker:worker /app

COPY --from=builder --chown=worker:worker /app/build/libs/*.jar /app/app.jar

USER worker

ENV SPRING_PROFILES_ACTIVE=prod \
    SPRING_THREADS_VIRTUAL_ENABLED=true \
    SPRING_MVC_SERVLET_LOAD_ON_STARTUP=1 \
    SPRING_MAIN_LAZY_INITIALIZATION=false \
    MINDLOG_PERFORMANCE_WARMUP_DB_ON_STARTUP=true \
    MINDLOG_LOGGING_REQUEST_LOG_NORMAL_INFO=false

ENV JAVA_OPTS="-XX:InitialRAMPercentage=25.0 \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Dfile.encoding=UTF-8 \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=10 \
  CMD wget -q -T 2 -O - http://127.0.0.1:8080/actuator/health/readiness | grep -q '"status":"UP"'

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
