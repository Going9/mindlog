import { Controller } from "@hotwired/stimulus"

const NOTICE_MESSAGE_BY_CODE = {
    "diary-created": "일기를 저장했어요.",
    "diary-updated": "일기를 수정했어요.",
    "diary-deleted": "일기를 삭제했어요.",
}
const NOTICE_META_SELECTOR = "meta[name='mindlog-notice-code']"

let toastTimer = null

function ensureToastHost() {
    let host = document.getElementById("mindlog-toast-host")
    if (host) {
        return host
    }

    host = document.createElement("div")
    host.id = "mindlog-toast-host"
    host.style.position = "fixed"
    host.style.left = "50%"
    host.style.bottom = "24px"
    host.style.transform = "translateX(-50%)"
    host.style.zIndex = "9999"
    host.style.pointerEvents = "none"
    document.body.appendChild(host)
    return host
}

function showToast(message, options = {}) {
    if (!message) {
        return
    }

    const host = ensureToastHost()
    host.innerHTML = ""

    const toast = document.createElement("div")
    toast.textContent = message
    toast.style.padding = "10px 14px"
    toast.style.borderRadius = "12px"
    toast.style.fontSize = "14px"
    toast.style.boxShadow = "0 8px 20px rgba(0, 0, 0, 0.18)"
    toast.style.background = "rgba(41, 37, 36, 0.94)"
    toast.style.color = "#fff"

    const tone = options.tone || "info"
    if (tone === "error") {
        toast.style.background = "rgba(127, 29, 29, 0.94)"
    } else if (tone === "success") {
        toast.style.background = "rgba(6, 78, 59, 0.94)"
    }

    host.appendChild(toast)

    if (toastTimer) {
        clearTimeout(toastTimer)
    }

    const duration = options.duration || 2600
    toastTimer = setTimeout(() => {
        host.innerHTML = ""
    }, duration)
}

export default class extends Controller {
    connect() {
        this.handleToastEvent = this.handleToastEvent.bind(this)
        this.handleBeforeCache = this.handleBeforeCache.bind(this)

        window.MindlogToast = showToast

        document.addEventListener("mindlog:toast", this.handleToastEvent)
        document.addEventListener("turbo:before-cache", this.handleBeforeCache)
        this.consumeNoticeCodeFromMeta()
    }

    disconnect() {
        document.removeEventListener("mindlog:toast", this.handleToastEvent)
        document.removeEventListener("turbo:before-cache", this.handleBeforeCache)
    }

    handleToastEvent(event) {
        const detail = event.detail || {}
        if (!detail.message) {
            return
        }

        showToast(detail.message, {
            tone: detail.tone || "info",
            duration: detail.duration || 2600,
        })
    }

    handleBeforeCache() {
        const host = document.getElementById("mindlog-toast-host")
        if (host) {
            host.innerHTML = ""
        }

        const noticeMeta = document.querySelector(NOTICE_META_SELECTOR)
        noticeMeta?.remove()
    }

    consumeNoticeCodeFromMeta() {
        const noticeMeta = document.querySelector(NOTICE_META_SELECTOR)
        const noticeCode = noticeMeta?.content?.trim()
        if (!noticeCode) {
            return
        }

        const message = NOTICE_MESSAGE_BY_CODE[noticeCode]
        if (message) {
            showToast(message, { tone: "success" })
        }
        noticeMeta.remove()
    }
}
