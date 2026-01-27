# 1단계: 빌드 스테이지
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Gradle 캐시 최적화
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# 보안을 위한 시스템 유저 설정 (UID 1000)
RUN addgroup -S worker -g 1000 && \
    adduser -S worker -u 1000 -G worker -D

# 빌드된 jar 복사 (소유권 이전)
COPY --from=builder --chown=worker:worker /app/build/libs/*.jar app.jar

# AppCDS 생성
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar || true

# 유저 전환
USER worker

# JVM 옵션
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:SharedArchiveFile=app.jsa \
               -Dspring.profiles.active=prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]