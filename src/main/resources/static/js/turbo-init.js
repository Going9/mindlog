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

    if (Turbo?.config?.forms) {
        Turbo.config.forms.confirm = (message) => {
            if (typeof window.MindlogConfirm === "function") {
                return window.MindlogConfirm(message)
            }

            return Promise.resolve(window.confirm(message))
        }
    }

    const loginRedirectUrl = () => {
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
    })

    document.addEventListener("turbo:before-fetch-response", (event) => {
        const status = event.detail.fetchResponse.status
        if ((status === 401 || status === 403) && window.location.pathname !== "/auth/login") {
            event.preventDefault()
            window.location.href = loginRedirectUrl()
        }
    })
}
