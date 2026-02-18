import { Controller } from "@hotwired/stimulus"

export default class extends Controller {
    static targets = ["source", "trigger", "label", "menu"]

    connect() {
        this.optionButtons = []
        this.activeIndex = -1
        this.isOpen = false

        this.handleOutsideClick = this.handleOutsideClick.bind(this)
        this.handleSourceChange = this.handleSourceChange.bind(this)
        this.handleViewportMove = this.handleViewportMove.bind(this)

        this.sourceTarget.classList.add("sr-only")
        this.sourceTarget.addEventListener("change", this.handleSourceChange)

        this.triggerTarget.setAttribute("aria-haspopup", "listbox")
        this.triggerTarget.setAttribute("aria-expanded", "false")
        if (!this.menuTarget.id) {
            this.menuTarget.id = `mindlog-select-${Math.random().toString(36).slice(2, 10)}`
        }
        this.triggerTarget.setAttribute("aria-controls", this.menuTarget.id)

        this.renderOptions()
        this.syncFromSource()

        document.addEventListener("click", this.handleOutsideClick)
        document.addEventListener("turbo:before-visit", this.handleBeforeVisit)
        document.addEventListener("turbo:before-cache", this.handleBeforeCache)
        window.addEventListener("scroll", this.handleViewportMove, true)
        window.addEventListener("resize", this.handleViewportMove)
        window.addEventListener("touchmove", this.handleViewportMove, true)
    }

    disconnect() {
        this.sourceTarget.removeEventListener("change", this.handleSourceChange)
        document.removeEventListener("click", this.handleOutsideClick)
        document.removeEventListener("turbo:before-visit", this.handleBeforeVisit)
        document.removeEventListener("turbo:before-cache", this.handleBeforeCache)
        window.removeEventListener("scroll", this.handleViewportMove, true)
        window.removeEventListener("resize", this.handleViewportMove)
        window.removeEventListener("touchmove", this.handleViewportMove, true)
        this.close(false)
    }

    toggle(event) {
        event.preventDefault()

        if (this.isOpen) {
            this.close(true)
        } else {
            this.open(true)
        }
    }

    select(event) {
        event.preventDefault()

        const index = Number(event.currentTarget.dataset.index)
        this.setSelectedIndex(index, true)
        this.close(true)
    }

    handleTriggerKeydown(event) {
        if (event.key === "ArrowDown") {
            event.preventDefault()
            if (!this.isOpen) {
                this.open(true)
            } else {
                this.move(1)
            }
            return
        }

        if (event.key === "ArrowUp") {
            event.preventDefault()
            if (!this.isOpen) {
                this.open(true)
            } else {
                this.move(-1)
            }
            return
        }

        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault()
            if (!this.isOpen) {
                this.open(true)
            } else if (this.activeIndex >= 0) {
                this.setSelectedIndex(this.activeIndex, true)
                this.close(true)
            }
        }
    }

    handleMenuKeydown(event) {
        if (!this.isOpen) {
            return
        }

        if (event.key === "Escape") {
            event.preventDefault()
            this.close(true)
            return
        }

        if (event.key === "ArrowDown") {
            event.preventDefault()
            this.move(1)
            return
        }

        if (event.key === "ArrowUp") {
            event.preventDefault()
            this.move(-1)
            return
        }

        if (event.key === "Enter") {
            event.preventDefault()
            if (this.activeIndex >= 0) {
                this.setSelectedIndex(this.activeIndex, true)
                this.close(true)
            }
        }
    }

    handleOutsideClick(event) {
        if (!this.isOpen) {
            return
        }

        if (!this.element.contains(event.target)) {
            this.close(false)
        }
    }

    handleSourceChange() {
        this.syncFromSource()
    }

    handleBeforeCache = () => {
        this.close(false)
    }

    handleBeforeVisit = () => {
        this.close(false)
    }

    handleViewportMove(event) {
        if (!this.isOpen) {
            return
        }

        if (event?.target instanceof Node && this.element.contains(event.target)) {
            return
        }

        this.close(false)
    }

    open(focusSelected) {
        this.isOpen = true
        this.menuTarget.classList.remove("hidden")
        this.triggerTarget.setAttribute("aria-expanded", "true")

        if (focusSelected) {
            const selectedIndex = this.sourceTarget.selectedIndex
            this.activate(selectedIndex >= 0 ? selectedIndex : 0)
        }
    }

    close(restoreFocus) {
        this.isOpen = false
        this.menuTarget.classList.add("hidden")
        this.triggerTarget.setAttribute("aria-expanded", "false")
        this.activeIndex = -1
        this.optionButtons.forEach((button) => button.classList.remove("is-active"))

        if (restoreFocus) {
            this.triggerTarget.focus()
        }
    }

    move(step) {
        if (this.optionButtons.length === 0) {
            return
        }

        const currentIndex = this.activeIndex >= 0 ? this.activeIndex : this.sourceTarget.selectedIndex
        const nextIndex = (currentIndex + step + this.optionButtons.length) % this.optionButtons.length
        this.activate(nextIndex)
    }

    activate(index) {
        if (index < 0 || index >= this.optionButtons.length) {
            return
        }

        this.activeIndex = index
        this.optionButtons.forEach((button, buttonIndex) => {
            button.classList.toggle("is-active", buttonIndex === index)
        })

        const targetButton = this.optionButtons[index]
        targetButton?.focus()
    }

    setSelectedIndex(index, dispatchChange) {
        if (index < 0 || index >= this.sourceTarget.options.length) {
            return
        }

        this.sourceTarget.selectedIndex = index
        this.syncFromSource()

        if (dispatchChange) {
            this.sourceTarget.dispatchEvent(new Event("change", { bubbles: true }))
        }
    }

    syncFromSource() {
        const selectedOption = this.sourceTarget.options[this.sourceTarget.selectedIndex]
        this.labelTarget.textContent = selectedOption ? selectedOption.textContent : "선택"

        this.optionButtons.forEach((button, index) => {
            button.classList.toggle("is-selected", index === this.sourceTarget.selectedIndex)
            button.setAttribute("aria-selected", index === this.sourceTarget.selectedIndex ? "true" : "false")
        })
    }

    renderOptions() {
        this.menuTarget.innerHTML = ""
        this.optionButtons = []

        Array.from(this.sourceTarget.options).forEach((option, index) => {
            const item = document.createElement("li")

            const button = document.createElement("button")
            button.type = "button"
            button.className = "mindlog-select-option"
            button.dataset.index = String(index)
            button.setAttribute("role", "option")
            button.setAttribute("aria-selected", "false")
            button.textContent = option.textContent
            button.addEventListener("click", this.select.bind(this))

            item.appendChild(button)
            this.menuTarget.appendChild(item)
            this.optionButtons.push(button)
        })
    }
}
