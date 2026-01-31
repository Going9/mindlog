import { Application, Controller } from "@hotwired/stimulus"

// 1. Stimulus 애플리케이션 시작
const application = Application.start()
window.Stimulus = application // 디버깅을 위해 전역 변수에 할당

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