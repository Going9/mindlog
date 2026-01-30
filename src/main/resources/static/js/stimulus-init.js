import { Application, Controller } from "@hotwired/stimulus"

// 1. Stimulus 애플리케이션 시작
const application = Application.start()
window.Stimulus = application // 디버깅을 위해 전역 변수에 할당

// ---------------------------------------------------------
// [핵심] 1. 외부 링크(로그인) 강제 이동 컨트롤러
// 역할: 카카오/구글 로그인 시 Turbo가 개입하지 못하게 차단하고 강제 이동
// ---------------------------------------------------------
class ExternalLinkController extends Controller {
    connect() {
        console.log("[Mindlog] 외부 링크 컨트롤러 연결됨:", this.element.href)
    }

    navigate(event) {
        // (1) 브라우저의 기본 이동 막기
        event.preventDefault()

        // (2) [중요] 이벤트가 상위(Turbo)로 전파되는 것을 즉시 중단
        event.stopImmediatePropagation()

        console.log("[Mindlog] Turbo 차단 -> 강제 페이지 이동 시도")

        // (3) 순수 자바스크립트로 이동 (CORS 헤더 없이 이동 -> 에러 해결)
        window.location.assign(this.element.href)
    }
}

// 'external-link'라는 이름으로 컨트롤러 등록
application.register("external-link", ExternalLinkController)


// ---------------------------------------------------------
// 2. Preline UI 초기화 컨트롤러 (기존 코드 유지)
// 역할: 페이지 이동 시 드롭다운, 모달 등 UI 기능이 깨지지 않게 재초기화
// ---------------------------------------------------------
application.register("preline", class extends Controller {
    connect() {
        // Turbo 네비게이션 시 Preline UI 구성 요소들이 깨지지 않도록 재초기화
        setTimeout(() => {
            // Optional Chaining(?.)을 사용하여 안전하게 호출
            window.HSStaticMethods?.autoInit?.();
        }, 100);
    }
})

console.log("[Mindlog] Stimulus 초기화 완료 (Local Module + ExternalLink Fix)")