import { Controller } from "@hotwired/stimulus"

export default class extends Controller {
    static targets = ["toggle", "panel"]

    connect() {
        this.shouldCloseOnNextRender = false
        this.pendingResetTimerId = null

        this.handleTurboLoad = this.handleTurboLoad.bind(this)
        this.handleBeforeCache = this.handleBeforeCache.bind(this)
        this.handleBeforeRender = this.handleBeforeRender.bind(this)

        document.addEventListener("turbo:load", this.handleTurboLoad)
        document.addEventListener("turbo:before-cache", this.handleBeforeCache)
        document.addEventListener("turbo:before-render", this.handleBeforeRender)
    }

    disconnect() {
        document.removeEventListener("turbo:load", this.handleTurboLoad)
        document.removeEventListener("turbo:before-cache", this.handleBeforeCache)
        document.removeEventListener("turbo:before-render", this.handleBeforeRender)
        this.clearPendingResetTimer()
    }

    scheduleCloseOnNavigate(event) {
        if (!this.isMobile()) {
            return
        }

        if (!(event.target instanceof Element)) {
            return
        }

        const activator = event.target.closest("a, button")
        if (!activator || (this.hasToggleTarget && activator === this.toggleTarget)) {
            return
        }

        this.shouldCloseOnNextRender = true
        this.startPendingResetTimer()
    }

    handleTurboLoad() {
        this.shouldCloseOnNextRender = false
        this.clearPendingResetTimer()
        this.close()
    }

    handleBeforeCache() {
        this.shouldCloseOnNextRender = false
        this.clearPendingResetTimer()
        this.close()
    }

    handleBeforeRender() {
        if (!this.shouldCloseOnNextRender) {
            return
        }
        this.shouldCloseOnNextRender = false
        this.clearPendingResetTimer()
        this.close()
    }

    startPendingResetTimer() {
        this.clearPendingResetTimer()
        this.pendingResetTimerId = window.setTimeout(() => {
            this.pendingResetTimerId = null
            this.shouldCloseOnNextRender = false
        }, 1500)
    }

    clearPendingResetTimer() {
        if (this.pendingResetTimerId !== null) {
            window.clearTimeout(this.pendingResetTimerId)
            this.pendingResetTimerId = null
        }
    }

    close() {
        if (!this.hasPanelTarget) {
            return
        }

        if (window.HSCollapse?.hide && this.hasToggleTarget) {
            try {
                window.HSCollapse.hide(this.toggleTarget)
            } catch (_) {
                // fallback below
            }
        }

        this.panelTarget.classList.add("hidden")
        this.panelTarget.classList.remove("open")
        this.panelTarget.style.height = ""

        if (this.hasToggleTarget) {
            this.toggleTarget.classList.remove("open")
            this.toggleTarget.setAttribute("aria-expanded", "false")
        }
    }

    isMobile() {
        return window.matchMedia("(max-width: 639px)").matches
    }
}
