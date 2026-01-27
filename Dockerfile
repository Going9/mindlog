# 1단계: 빌드 스테이지
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# 실행 사용자 생성 및 'logs' 디렉토리 권한 사전 설정
RUN addgroup -S mindlog && adduser -S mindlog -G mindlog && \
    mkdir -p /app/logs && \
    chown -R mindlog:mindlog /app && \
    chmod -R 755 /app/logs

# 이후 모든 작업은 mindlog 사용자로 실행
USER mindlog

COPY --from=build /app/build/libs/*.jar app.jar

# JVM 최적화 옵션
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=prod"

EXPOSE 8080

# 로그 파일이 위치할 상대 경로가 /app/logs가 되도록 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]