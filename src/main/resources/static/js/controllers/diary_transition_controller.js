import { Controller } from "@hotwired/stimulus"

export default class extends Controller {
    static values = {
        minVisibleMs: { type: Number, default: 500 }
    }

    connect() {
        this.pendingTransition = null
        this.resumeTimerId = null

        this.onClick = this.handleClick.bind(this)
        this.onTurboVisit = this.handleTurboVisit.bind(this)
        this.onBeforeRender = this.handleBeforeRender.bind(this)
        this.onTurboLoad = this.handleTurboLoad.bind(this)

        this.element.addEventListener("click", this.onClick)
        document.addEventListener("turbo:visit", this.onTurboVisit)
        document.addEventListener("turbo:before-render", this.onBeforeRender)
        document.addEventListener("turbo:load", this.onTurboLoad)
    }

    disconnect() {
        this.element.removeEventListener("click", this.onClick)
        document.removeEventListener("turbo:visit", this.onTurboVisit)
        document.removeEventListener("turbo:before-render", this.onBeforeRender)
        document.removeEventListener("turbo:load", this.onTurboLoad)

        this.clearResumeTimer()
        this.pendingTransition = null
    }

    handleClick(event) {
        if (!this.isNativeApp()) {
            return
        }
        if (event.defaultPrevented) {
            return
        }
        if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return
        }
        if (!(event.target instanceof Element)) {
            return
        }

        const anchor = event.target.closest("a[data-diary-transition='detail']")
        if (!anchor || !this.element.contains(anchor)) {
            return
        }

        const targetUrl = new URL(anchor.href, window.location.origin)
        if (targetUrl.origin !== window.location.origin) {
            return
        }
        if (!this.isDiaryDetailPath(targetUrl.pathname)) {
            return
        }

        this.pendingTransition = {
            targetPath: targetUrl.pathname,
            startedAt: performance.now(),
            matchedVisit: false
        }
    }

    handleTurboVisit(event) {
        if (!this.pendingTransition) {
            return
        }

        const url = event.detail?.url
        if (typeof url !== "string") {
            return
        }

        const visitPath = new URL(url, window.location.origin).pathname
        if (visitPath === this.pendingTransition.targetPath) {
            this.pendingTransition.matchedVisit = true
        }
    }

    handleBeforeRender(event) {
        if (!this.pendingTransition || !this.pendingTransition.matchedVisit) {
            return
        }

        const elapsedMs = performance.now() - this.pendingTransition.startedAt
        const remainingMs = Math.ceil(this.minVisibleMsValue - elapsedMs)
        if (remainingMs <= 0) {
            this.pendingTransition = null
            return
        }

        event.preventDefault()
        this.clearResumeTimer()
        this.resumeTimerId = window.setTimeout(() => {
            this.resumeTimerId = null
            this.pendingTransition = null
            event.detail.resume()
        }, remainingMs)
    }

    handleTurboLoad() {
        this.pendingTransition = null
    }

    clearResumeTimer() {
        if (this.resumeTimerId !== null) {
            window.clearTimeout(this.resumeTimerId)
            this.resumeTimerId = null
        }
    }

    isNativeApp() {
        return document.body.classList.contains("is-native")
    }

    isDiaryDetailPath(pathname) {
        return /^\/diaries\/\d+$/.test(pathname)
    }
}
