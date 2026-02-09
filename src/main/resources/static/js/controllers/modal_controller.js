import { Controller } from "@hotwired/stimulus"

/**
 * 범용 모달 Controller
 * 
 * 모달의 열기/닫기/배경 클릭 닫기를 관리합니다.
 * 
 * 사용법:
 * <div data-controller="modal">
 *   <button data-action="click->modal#open">열기</button>
 *   
 *   <div data-modal-target="dialog" class="hidden fixed inset-0 z-50">
 *     <div class="fixed inset-0 bg-black/50" data-action="click->modal#backdropClose"></div>
 *     <div class="modal-content">
 *       <button data-action="click->modal#close">닫기</button>
 *       ...
 *     </div>
 *   </div>
 * </div>
 */
export default class extends Controller {
    static targets = ["dialog"]

    static values = {
        closeOnBackdrop: { type: Boolean, default: true }
    }

    /**
     * 모달 열기
     */
    open() {
        if (!this.hasDialogTarget) return

        this.dialogTarget.classList.remove('hidden')
        document.body.style.overflow = 'hidden'

        this.dispatch('opened')
    }

    /**
     * 모달 닫기
     */
    close() {
        if (!this.hasDialogTarget) return

        this.dialogTarget.classList.add('hidden')
        document.body.style.overflow = ''

        this.dispatch('closed')
    }

    /**
     * 배경 클릭 시 닫기
     */
    backdropClose(event) {
        if (!this.closeOnBackdropValue) return

        // 클릭된 요소가 배경인 경우에만 닫기
        if (event.target === event.currentTarget) {
            this.close()
        }
    }

    /**
     * Escape 키로 닫기
     */
    closeWithKeyboard(event) {
        if (event.key === 'Escape') {
            this.close()
        }
    }
}
