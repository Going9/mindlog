import { Controller } from "@hotwired/stimulus"

export default class extends Controller {
    static targets = ["message", "confirmButton", "cancelButton"]

    connect() {
        this.boundConfirm = this.openConfirm.bind(this)
        this.resolvePromise = null

        window.MindlogConfirm = this.boundConfirm
        document.addEventListener("turbo:before-cache", this.handleBeforeCache)
    }

    disconnect() {
        document.removeEventListener("turbo:before-cache", this.handleBeforeCache)

        if (window.MindlogConfirm === this.boundConfirm) {
            delete window.MindlogConfirm
        }

        this.finish(false)
    }

    openConfirm(message) {
        if (this.resolvePromise) {
            this.resolvePromise(false)
            this.resolvePromise = null
        }

        if (this.hasMessageTarget) {
            this.messageTarget.textContent = message || "계속 진행할까요?"
        }

        this.element.classList.remove("hidden")
        document.body.classList.add("overflow-hidden")

        window.setTimeout(() => {
            if (this.hasCancelButtonTarget) {
                this.cancelButtonTarget.focus()
            }
        }, 0)

        return new Promise((resolve) => {
            this.resolvePromise = resolve
        })
    }

    confirm(event) {
        event?.preventDefault()
        this.finish(true)
    }

    cancel(event) {
        event?.preventDefault()
        this.finish(false)
    }

    handleWindowKeydown = (event) => {
        if (this.element.classList.contains("hidden")) {
            return
        }

        if (event.key === "Escape") {
            event.preventDefault()
            this.finish(false)
            return
        }

        if (event.key === "Enter" && document.activeElement !== this.cancelButtonTarget) {
            event.preventDefault()
            this.finish(true)
        }
    }

    handleBeforeCache = () => {
        this.finish(false)
    }

    finish(result) {
        this.element.classList.add("hidden")
        document.body.classList.remove("overflow-hidden")

        if (this.resolvePromise) {
            this.resolvePromise(result)
            this.resolvePromise = null
        }
    }
}
