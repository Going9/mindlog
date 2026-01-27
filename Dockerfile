# 1단계: 빌드 스테이지 (Java 25 JDK 사용)
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
# gradlew 실행 권한 부여
RUN chmod +x ./gradlew
# 소스 복사 및 빌드
COPY src src
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지 (Java 25 JRE 사용)
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# 시스템 유저 생성
RUN addgroup -S worker && adduser -S worker -G worker

# 앱이 실행될 /app 폴더 전체의 소유권을 worker 유저에게 부여
RUN chown -R worker:worker /app

# 보안을 위해 유저 전환
USER worker

# 빌드된 jar 복사 (소유권 유지)
COPY --from=builder --chown=worker:worker /app/build/libs/*.jar app.jar

# JVM 최적화 및 로그 경로 강제 지정
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=prod"

EXPOSE 8080

# 쉘 형식(sh -c)을 사용하여 환경 변수가 제대로 확장되도록 설정
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]