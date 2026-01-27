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

# 실행 사용자 생성 및 로그 디렉토리 권한 설정
RUN addgroup -S mindlog && adduser -S mindlog -G mindlog && \
    mkdir logs && \
    chown -R mindlog:mindlog /app

# 이제부터 mindlog 사용자로 작업 실행
USER mindlog

COPY --from=build /app/build/libs/*.jar app.jar

# JAVA_OPTS 수정: 경고가 발생하는 +ZGenerational 제거 (Java 25에서는 기본)
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]