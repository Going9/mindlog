# 작업 로그: 운영 런타임 오류(ChannelDecisionManager) 긴급 수정 (2026-02-10)

## 작업 내용
클라우드타입 운영 기동 중 `NoClassDefFoundError: ChannelDecisionManager`가 발생하여 앱이 시작되지 않는 문제를 수정했습니다.

1. **원인 확인**
   - 파일: `src/main/java/com/mindlog/global/config/SecurityConfig.java`
   - 원인: `requiresChannel()` 사용 시 런타임에서 `org.springframework.security.web.access.channel.ChannelDecisionManager` 클래스를 찾지 못해 `filterChain` 빈 생성 실패.

2. **긴급 조치**
   - 파일: `src/main/java/com/mindlog/global/config/SecurityConfig.java`
   - 변경: `http.requiresChannel(...)` 제거
   - 유지: 운영 플래그(`mindlog.security.require-https=true`)일 때 `HSTS` 헤더 적용은 유지

3. **운영 HTTPS 처리 방식**
   - 애플리케이션 내부 강제 리다이렉트 대신, 클라우드타입의 HTTP -> HTTPS 리다이렉트 설정에 위임
   - 앱은 HSTS로 브라우저의 HTTPS 재접속을 보조

## 검증
- 실행 명령: `./gradlew build -x test`
- 결과: `BUILD SUCCESSFUL`

## 확인 필요
- 클라우드타입 콘솔에서 HTTP -> HTTPS 리다이렉트 옵션이 활성화되어 있는지 점검 필요.
