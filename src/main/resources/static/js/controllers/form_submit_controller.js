import { Controller } from "@hotwired/stimulus"

/**
 * 폼 제출 로딩 상태 Controller
 * 
 * 사용법:
 * <form data-controller="form-submit" data-action="submit->form-submit#submit">
 *   <button type="submit" 
 *           data-form-submit-target="submit"
 *           data-form-submit-loading-text-value="저장 중...">
 *     <svg data-form-submit-target="spinner" class="hidden ...">...</svg>
 *     <span data-form-submit-target="text">저장하기</span>
 *   </button>
 * </form>
 */
export default class extends Controller {
    static targets = ["submit", "spinner", "text"]
    static values = {
        loadingText: { type: String, default: "저장 중..." }
    }

    submit() {
        if (this.hasSubmitTarget) {
            this.submitTarget.disabled = true
        }

        if (this.hasSpinnerTarget) {
            this.spinnerTarget.classList.remove("hidden")
        }

        if (this.hasTextTarget) {
            this.textTarget.textContent = this.loadingTextValue
        }
    }

    // 폼 제출 실패 시 상태 복구
    reset() {
        if (this.hasSubmitTarget) {
            this.submitTarget.disabled = false
        }

        if (this.hasSpinnerTarget) {
            this.spinnerTarget.classList.add("hidden")
        }
    }
}
