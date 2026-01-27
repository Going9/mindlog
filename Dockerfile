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

# 1. 유저와 그룹 생성
# 2. 로그 폴더(하위 archived 포함) 미리 생성
# 3. /app 폴더 전체의 소유권을 mindlog 유저에게 양도
RUN addgroup -S mindlog && adduser -S mindlog -G mindlog && \
    mkdir -p /app/logs/archived && \
    chown -R mindlog:mindlog /app

# 이제부터는 mindlog 유저로 작업 (보안 강화)
USER mindlog

COPY --from=build /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=prod -DLOG_PATH=/tmp"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]