# 작업 로그: 클라우드타입 HTTPS 경고 대응 (2026-02-10)

## 작업 내용
클라우드타입 배포 환경에서 발생하던 HTTPS 경고를 줄이기 위해, 프록시 헤더 처리와 보안 헤더 정책을 강화했습니다.

1. **Forwarded 헤더 처리 전략 변경**
   - 파일: `src/main/resources/application-prod.yaml`
   - 변경: `server.forward-headers-strategy: native -> framework`
   - 이유: 프록시(클라우드타입) 뒤에서 `X-Forwarded-Proto`를 Spring이 더 일관되게 반영하도록 설정.

2. **HTTPS 강제 리다이렉트 설정 추가**
   - 파일: `src/main/java/com/mindlog/global/config/SecurityConfig.java`
   - 변경: `requiresChannel(anyRequest().requiresSecure())` 추가
   - 이유: 모든 HTTP 요청을 HTTPS로 강제하여 혼합 프로토콜 접근으로 인한 경고를 방지.

3. **HSTS 설정 추가**
   - 파일: `src/main/java/com/mindlog/global/config/SecurityConfig.java`
   - 변경: `httpStrictTransportSecurity(maxAge=31536000, includeSubDomains=true)` 추가
   - 이유: 브라우저가 이후 요청을 HTTPS로만 보내도록 유도하여 보안 경고 재발 가능성 축소.

## 검증
- 실행 명령: `./gradlew build -x test`
- 결과: `BUILD SUCCESSFUL`
- 참고: `SecurityConfig`의 deprecated API 경고가 출력되었으나 빌드는 정상 완료.

## 남은 확인 사항
- 클라우드타입 콘솔에서 인증서 상태(발급 완료)와 HTTP -> HTTPS 리다이렉트 옵션이 활성화되어 있는지 확인 필요.
- 커스텀 도메인 DNS 설정(`www` 포함 여부)이 인증서 대상과 일치하는지 확인 필요.
