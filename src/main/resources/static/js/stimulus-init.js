// [변경] CDN URL 대신 별칭(@hotwired/stimulus) 사용
import { Application, Controller } from "@hotwired/stimulus"

// Stimulus 애플리케이션 시작
const application = Application.start()
window.Stimulus = application // 디버깅을 위해 전역 변수에 할당

// Preline UI 컨트롤러 등록
application.register("preline", class extends Controller {
    connect() {
        // Turbo 네비게이션 시 Preline UI 구성 요소들이 깨지지 않도록 재초기화
        setTimeout(() => {
            window.HSStaticMethods?.removeCollection?.();
            window.HSStaticMethods?.autoInit?.();
        }, 100);
    }
})

console.log("[Mindlog] Stimulus 초기화 완료 (Local Module)");