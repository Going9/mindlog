# 작업 로그: 운영/로컬 HTTPS 설정 분리 (2026-02-10)

## 작업 내용
운영 환경은 HTTPS를 강제하고, 로컬 개발 환경은 HTTP로 동작하도록 보안 설정을 프로필별로 분리했습니다.

1. **보안 설정 조건부 적용**
   - 파일: `src/main/java/com/mindlog/global/config/SecurityConfig.java`
   - 변경: `mindlog.security.require-https` 플래그를 주입받아, `true`일 때만 아래 정책 적용
     - `requiresChannel(anyRequest().requiresSecure())`
     - `HSTS(maxAge=31536000, includeSubDomains=true)`
   - 이유: 로컬 환경에서 불필요한 HTTPS 강제 리다이렉트를 방지.

2. **프로필별 설정 값 분리**
   - 파일: `src/main/resources/application-prod.yaml`
     - `mindlog.security.require-https: true`
   - 파일: `src/main/resources/application-local.yaml`
     - `mindlog.security.require-https: false`
   - 이유: 운영/로컬 환경 요구사항을 명확히 분리하여 배포 안정성과 개발 편의성을 동시에 확보.

## 검증
- 실행 명령: `./gradlew build -x test`
- 결과: `BUILD SUCCESSFUL`
- 참고: deprecated API 경고는 기존과 동일하게 출력되나 빌드에는 영향 없음.
