import { Controller } from "@hotwired/stimulus"

export default class extends Controller {
    static targets = ["input"]
    static values = {
        format: { type: String, default: "Y-m-d" }
    }

    connect() {
        this.instance = null
        document.addEventListener("turbo:before-cache", this.handleBeforeCache)
        this.initializePicker()
    }

    disconnect() {
        document.removeEventListener("turbo:before-cache", this.handleBeforeCache)
        this.destroyPicker()
    }

    handleBeforeCache = () => {
        this.destroyPicker()
    }

    initializePicker() {
        const input = this.hasInputTarget ? this.inputTarget : this.element
        if (!input || typeof window.flatpickr !== "function") {
            return
        }

        const locale = window.flatpickr?.l10ns?.ko ? "ko" : "default"
        this.instance = window.flatpickr(input, {
            dateFormat: this.formatValue,
            locale,
            allowInput: true,
            disableMobile: true
        })
    }

    destroyPicker() {
        if (this.instance) {
            this.instance.destroy()
            this.instance = null
        }
    }
}
