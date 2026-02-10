import { Application, Controller } from "@hotwired/stimulus"
import FormSubmitController from "./controllers/form_submit_controller.js"
import TagController from "./controllers/tag_controller.js"
import ModalController from "./controllers/modal_controller.js"

// 1. Stimulus 애플리케이션 싱글톤 시작
// Turbo 네비게이션/스크립트 재실행 시 중복 start()가 발생하면
// 동일 액션이 2번 바인딩될 수 있어 전역 싱글톤으로 보호합니다.
const existingApplication = window.__mindlogStimulusApplication
const application = existingApplication ?? Application.start()

if (!existingApplication) {
    window.__mindlogStimulusApplication = application
    window.Stimulus = application // 디버깅을 위해 전역 변수에 할당
}

// ---------------------------------------------------------
// 2. Controllers 등록
// ---------------------------------------------------------

// 폼 제출 로딩 상태 컨트롤러
if (!existingApplication) {
    application.register("form-submit", FormSubmitController)

    // 태그 관리 컨트롤러
    application.register("tag", TagController)

    // 범용 모달 컨트롤러
    application.register("modal", ModalController)

    // Preline UI 초기화 컨트롤러 (기존 코드 유지)
    // 역할: 페이지 이동 시 드롭다운, 모달 등 UI 기능이 깨지지 않게 재초기화
    application.register("preline", class extends Controller {
        connect() {
            // Turbo 네비게이션 시 Preline UI 구성 요소들이 깨지지 않도록 재초기화
            setTimeout(() => {
                // Optional Chaining(?.)을 사용하여 안전하게 호출
                window.HSStaticMethods?.autoInit?.();
            }, 100);
        }
    })

    console.log("[Mindlog] Stimulus 초기화 완료 (FormSubmit Controller 포함)")
} else {
    console.log("[Mindlog] Stimulus 재초기화 요청 감지 - 기존 인스턴스 재사용")
}
