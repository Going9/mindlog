# 1단계: 빌드 스테이지
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
COPY src src
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# 시스템 유저 생성
RUN addgroup -S worker && adduser -S worker -G worker

# 앱 실행 전 로그 폴더를 '미리' 생성하고 소유권을 worker에게 부여
RUN mkdir -p /app/logs && \
    chown -R worker:worker /app && \
    chmod -R 755 /app/logs

# 유저 전환
USER worker

# 빌드된 jar 복사 (소유권 유지)
COPY --from=builder --chown=worker:worker /app/build/libs/*.jar app.jar

# JVM 옵션
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]