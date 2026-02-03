# 1단계: 빌드 스테이지
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# 빌드에 필요한 시스템 종속성 추가 (Node.js 및 npm 설치)
# Alpine 환경이므로 apk를 사용
RUN apk add --no-cache nodejs npm

# Gradle 캐시 효율을 위해 설정 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 복사 및 빌드
COPY src src
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# 시스템 유저 생성 및 앱 폴더 소유권 부여
RUN addgroup -S worker -g 1000 && \
    adduser -S worker -u 1000 -G worker -D && \
    chown -R worker:worker /app

# 빌드된 jar 복사 (소유권 반영)
COPY --from=builder --chown=worker:worker /app/build/libs/*.jar app.jar

# worker 유저로 전환 후 CDS 생성 작업을 수행
USER worker

# AppCDS 생성 (AOT)
# 기본 GC(G1GC)를 사용하므로 별도의 GC 옵션 없이 실행
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

# 실행 옵션
# -XX:+UseZGC를 제거하여 기본 G1GC를 사용
# -XX:SharedArchiveFile을 통해 위에서 생성한 app.jsa를 적용
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:SharedArchiveFile=app.jsa \
               -Dspring.profiles.active=prod"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]