import * as Turbo from "@hotwired/turbo"

function initSupabaseForTurbo(supabaseUrl, supabaseAnonKey) {
    if (!supabaseUrl || !supabaseAnonKey) {
        return null
    }

    if (window.supabaseClient) {
        return window.supabaseClient
    }

    const createClient = window.supabase?.createClient
    if (typeof createClient !== "function") {
        console.warn("[Mindlog] Supabase SDK를 찾을 수 없어 클라이언트를 초기화하지 못했습니다.")
        return null
    }

    const client = createClient(supabaseUrl, supabaseAnonKey)
    window.supabaseClient = client
    return client
}

window.initSupabaseForTurbo = initSupabaseForTurbo
window.Turbo = Turbo

if (!window.__mindlogTurboInitialized) {
    window.__mindlogTurboInitialized = true

    Turbo.start()

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

    window.MindlogToast = showToast
    window.MindlogConfirm = (message) => Promise.resolve(window.confirm(message))

    function consumeNoticeCodeFromUrl() {
        const params = new URLSearchParams(window.location.search)
        const noticeCode = params.get("noticeCode")
        if (!noticeCode) {
            return
        }

        const messageByCode = {
            "diary-created": "일기를 저장했어요.",
            "diary-updated": "일기를 수정했어요.",
            "diary-deleted": "일기를 삭제했어요.",
        }

        const message = messageByCode[noticeCode]
        if (message) {
            showToast(message, { tone: "success" })
        }

        params.delete("noticeCode")
        const query = params.toString()
        const nextUrl = `${window.location.pathname}${query ? `?${query}` : ""}${window.location.hash}`
        window.history.replaceState({}, "", nextUrl)
    }

    function loginRedirectUrl() {
        const params = new URLSearchParams({ error: "session_expired" })
        if (document.body.classList.contains("is-native")) {
            params.set("source", "app")
        }
        return `/auth/login?${params.toString()}`
    }

    document.addEventListener("turbo:load", () => {
        if (typeof HSStaticMethods !== "undefined") {
            HSStaticMethods.autoInit()
        }
        consumeNoticeCodeFromUrl()
    })

    document.addEventListener("mindlog:toast", (event) => {
        const detail = event.detail || {}
        if (detail.message) {
            showToast(detail.message, {
                tone: detail.tone || "info",
                duration: detail.duration || 2600,
            })
        }
    })

    document.addEventListener("turbo:before-fetch-response", (event) => {
        const status = event.detail.fetchResponse.status
        if ((status === 401 || status === 403) && window.location.pathname !== "/auth/login") {
            event.preventDefault()
            window.location.href = loginRedirectUrl()
        }
    })

    if (Turbo?.config?.forms) {
        Turbo.config.forms.confirm = (message) => window.MindlogConfirm(message)
    }
}
