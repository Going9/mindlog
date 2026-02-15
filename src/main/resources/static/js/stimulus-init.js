import { Application } from "@hotwired/stimulus"
import ConfirmModalController from "./controllers/confirm_modal_controller.js"
import CustomSelectController from "./controllers/custom_select_controller.js"
import DatePickerController from "./controllers/date_picker_controller.js"
import DiaryTransitionController from "./controllers/diary_transition_controller.js"
import FlashNoticeController from "./controllers/flash_notice_controller.js"
import FormSubmitController from "./controllers/form_submit_controller.js"
import ModalController from "./controllers/modal_controller.js"
import NavbarCollapseController from "./controllers/navbar_collapse_controller.js"
import TagController from "./controllers/tag_controller.js"

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
    application.register("confirm-modal", ConfirmModalController)
    application.register("custom-select", CustomSelectController)
    application.register("date-picker", DatePickerController)
    application.register("navbar-collapse", NavbarCollapseController)

    application.register("flash-notice", FlashNoticeController)

    application.register("form-submit", FormSubmitController)

    // 태그 관리 컨트롤러
    application.register("tag", TagController)

    // 범용 모달 컨트롤러
    application.register("modal", ModalController)

    application.register("diary-transition", DiaryTransitionController)

    console.log("[Mindlog] Stimulus 초기화 완료 (FormSubmit Controller 포함)")
} else {
    console.log("[Mindlog] Stimulus 재초기화 요청 감지 - 기존 인스턴스 재사용")
}
