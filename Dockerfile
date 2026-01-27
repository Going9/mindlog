# 1단계: 빌드 스테이지
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Gradle 캐시 효율을 위해 설정 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# 의존성 먼저 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# [보안] 클라우드타입 권장 비루트 유저 설정 (UID 1000)
RUN addgroup -S worker -g 1000 && \
    adduser -S worker -u 1000 -G worker -D

# 빌드된 jar 복사 및 소유권 변경
COPY --from=builder --chown=worker:worker /app/build/libs/*.jar app.jar

# AppCDS 생성 (AOT)
# 더미 환경 변수를 주입하여 스프링 컨텍스트 로딩만 수행
RUN DB_URL=jdbc:postgresql://localhost:5432/dummy \
    DB_USERNAME=dummy \
    DB_PASSWORD=dummy \
    SUPABASE_URL=dummy \
    SUPABASE_ANON_KEY=dummy \
    SERVER_IP=127.0.0.1 \
    SERVER_PORT=8080 \
    java -XX:ArchiveClassesAtExit=app.jsa \
         -Dspring.profiles.active=prod \
         -Dspring.context.exit=onRefresh \
         -jar app.jar || true

# 유저 전환
USER worker

# [성능] Java 25 가상 스레드 및 저지연 성능을 위한 최적 옵션
# -XX:+UseZGC: 가상 스레드 최적화 GC
# -XX:SharedArchiveFile: 빌드 시 생성한 app.jsa를 사용하여 기동 속도 향상
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:SharedArchiveFile=app.jsa \
               -Dspring.profiles.active=prod"

EXPOSE 8080

# 클라우드타입 환경 변수 확장을 지원하는 실행 방식
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]