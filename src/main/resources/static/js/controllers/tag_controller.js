import { Controller } from "@hotwired/stimulus"
import { createTagRequest, deleteTagRequest } from "./tag/tag_api.js"
import {
    addTagToSelection,
    buildTagItemElement,
    markTagSelected,
    removeTagFromSelection,
    removeTagItem,
    syncSelectedButtons,
    toggleTagSelection
} from "./tag/tag_dom.js"

/**
 * 태그 관리 Stimulus Controller
 *
 * 태그 선택/해제, 모달 열기/닫기, 태그 생성 API 호출을 관리합니다.
 */
export default class extends Controller {
    static targets = [
        "modal",
        "tagList",
        "selectedContainer",
        "tagName",
        "tagCategory",
        "colorPicker",
        "colorValue",
        "selectedColorPreview",
        "errorMessage",
        "submitBtn",
        "spinner",
        "submitText",
        "notice"
    ]

    static values = {
        colors: {
            type: Array,
            default: [
                "#EF4444", "#F97316", "#F59E0B", "#10B981", "#06B6D4",
                "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899", "#78716C"
            ]
        },
        apiEndpoint: { type: String, default: "/api/tags" },
        loadingText: { type: String, default: "추가 중..." },
        defaultCategory: { type: String, default: "POSITIVE" },
        defaultColor: { type: String, default: "#78716C" }
    }

    connect() {
        this.isCreating = false
        this.deletingTagIds = new Set()
        this.noticeTimer = null

        if (this.hasSelectedContainerTarget && this.hasTagListTarget) {
            syncSelectedButtons(this.selectedContainerTarget, this.tagListTarget)
        }
    }

    toggle(event) {
        event.preventDefault()
        if (!this.hasSelectedContainerTarget) {
            return
        }

        toggleTagSelection(this.selectedContainerTarget, event.currentTarget)
    }

    openModal(event) {
        event?.preventDefault()
        if (!this.hasModalTarget) {
            return
        }

        this.modalTarget.classList.remove("hidden")
        this._clearError()
        this._initColorPicker()
        this._applySelectedColor(this.defaultColorValue)
    }

    closeModal(event) {
        event?.preventDefault()
        if (!this.hasModalTarget) {
            return
        }

        this.modalTarget.classList.add("hidden")
        this._resetForm()
    }

    selectColor(event) {
        event.preventDefault()
        this._applySelectedColor(event.currentTarget.dataset.color)
    }

    async createTag(event) {
        event?.preventDefault()
        if (this.isCreating) {
            return
        }

        const name = this.hasTagNameTarget ? this.tagNameTarget.value.trim() : ""
        const category = this.hasTagCategoryTarget ? this.tagCategoryTarget.value : this.defaultCategoryValue
        const color = this.hasColorValueTarget && this.colorValueTarget.value
            ? this.colorValueTarget.value
            : this.defaultColorValue

        if (!name) {
            this._showError("이름을 입력해주세요.")
            return
        }

        this.isCreating = true
        this._setLoading(true)

        try {
            const response = await createTagRequest(this.apiEndpointValue, { name, color, category })

            if (!response.ok) {
                const errorText = await response.text()
                this._showError(errorText || "태그 생성 실패")
                this._showNotice(errorText || "태그 생성에 실패했어요.", "error")
                return
            }

            const newTag = await response.json()
            this.closeModal()
            this._addTagToList(newTag)
            this._showNotice("새 감정 태그를 추가했어요.", "success")
        } catch (e) {
            console.error(e)
            this._showError("오류가 발생했습니다: " + e.message)
            this._showNotice("요청 처리 중 오류가 발생했어요.", "error")
        } finally {
            this._setLoading(false)
            this.isCreating = false
        }
    }

    async deleteTag(event) {
        event.preventDefault()
        event.stopPropagation()

        const btn = event.currentTarget
        const tagId = btn?.dataset?.tagId
        const tagName = btn?.dataset?.tagName || "이 태그"
        if (!tagId || this.deletingTagIds.has(tagId)) {
            return
        }

        const confirmed = await this._confirm(`"${tagName}" 태그를 삭제할까요?`)
        if (!confirmed) {
            return
        }

        this.deletingTagIds.add(tagId)
        btn.disabled = true

        try {
            const response = await deleteTagRequest(this.apiEndpointValue, tagId)

            if (!response.ok) {
                const errorText = await response.text()
                this._showError(errorText || "태그 삭제 실패")
                this._showNotice(errorText || "태그 삭제에 실패했어요.", "error")
                btn.disabled = false
                return
            }

            if (this.hasSelectedContainerTarget) {
                removeTagFromSelection(this.selectedContainerTarget, tagId)
            }
            if (this.hasTagListTarget) {
                removeTagItem(this.tagListTarget, tagId)
            }
            this._clearError()
            this._showNotice(`"${tagName}" 태그를 삭제했어요.`, "success")
        } catch (e) {
            console.error(e)
            this._showError("오류가 발생했습니다: " + e.message)
            this._showNotice("요청 처리 중 오류가 발생했어요.", "error")
            btn.disabled = false
        } finally {
            this.deletingTagIds.delete(tagId)
        }
    }

    _initColorPicker() {
        if (!this.hasColorPickerTarget || this.colorPickerTarget.children.length > 0) {
            return
        }

        this.colorsValue.forEach(color => {
            const btn = document.createElement("button")
            btn.type = "button"
            btn.className = "w-10 h-10 rounded-full border-2 border-white shadow-sm cursor-pointer transition-transform hover:scale-110 color-option ring-2 ring-transparent hover:ring-gray-300"
            btn.style.backgroundColor = color
            btn.dataset.color = color
            btn.dataset.action = "click->tag#selectColor"
            btn.setAttribute("data-action", "click->tag#selectColor")
            this.colorPickerTarget.appendChild(btn)
        })
    }

    _addTagToList(tag) {
        if (!this.hasTagListTarget) {
            return
        }

        const existingBtn = this.tagListTarget.querySelector(`button[data-tag-id="${tag.id}"]`)
        if (existingBtn) {
            this._selectNewTag(tag, existingBtn)
            return
        }

        const { wrapper, tagButton } = buildTagItemElement(tag)
        const addButton = this.tagListTarget.lastElementChild
        if (addButton) {
            this.tagListTarget.insertBefore(wrapper, addButton)
        } else {
            this.tagListTarget.appendChild(wrapper)
        }

        this._selectNewTag(tag, tagButton)
    }

    _selectNewTag(tag, button) {
        if (!this.hasSelectedContainerTarget) {
            return
        }

        addTagToSelection(this.selectedContainerTarget, tag)
        markTagSelected(button, tag.color)
    }

    _setLoading(isLoading) {
        if (this.hasSubmitBtnTarget) {
            this.submitBtnTarget.disabled = isLoading
        }

        if (this.hasSpinnerTarget) {
            this.spinnerTarget.classList.toggle("hidden", !isLoading)
        }

        if (this.hasSubmitTextTarget) {
            this.submitTextTarget.textContent = isLoading ? this.loadingTextValue : "추가하기"
        }
    }

    _showError(message) {
        if (!this.hasErrorMessageTarget) {
            return
        }

        this.errorMessageTarget.innerText = message
        this.errorMessageTarget.classList.remove("hidden")
    }

    _clearError() {
        if (!this.hasErrorMessageTarget) {
            return
        }

        this.errorMessageTarget.innerText = ""
        this.errorMessageTarget.classList.add("hidden")
    }

    _showNotice(message, type = "info") {
        if (typeof window.MindlogToast === "function") {
            const tone = type === "success" ? "success" : type === "error" ? "error" : "info"
            window.MindlogToast(message, { tone })
            return
        }

        if (!this.hasNoticeTarget) {
            return
        }

        if (this.noticeTimer) {
            clearTimeout(this.noticeTimer)
            this.noticeTimer = null
        }

        const baseClass = "mb-3 rounded-lg border px-3 py-2 text-sm"
        const successClass = "border-emerald-200 bg-emerald-50/80 text-emerald-800"
        const errorClass = "border-rose-200 bg-rose-50/80 text-rose-800"
        const infoClass = "border-stone-200 bg-white/80 text-stone-700"

        this.noticeTarget.className = `${baseClass} ${type === "success" ? successClass : type === "error" ? errorClass : infoClass}`
        this.noticeTarget.textContent = message
        this.noticeTarget.classList.remove("hidden")

        this.noticeTimer = setTimeout(() => {
            this.noticeTarget.classList.add("hidden")
            this.noticeTarget.textContent = ""
        }, 2500)
    }

    async _confirm(message) {
        if (typeof window.MindlogConfirm === "function") {
            return await window.MindlogConfirm(message)
        }
        return window.confirm(message)
    }

    _resetForm() {
        if (this.hasTagNameTarget) {
            this.tagNameTarget.value = ""
        }

        if (this.hasTagCategoryTarget) {
            this.tagCategoryTarget.value = this.defaultCategoryValue
            this.tagCategoryTarget.dispatchEvent(new Event("change", { bubbles: true }))
        }

        this._applySelectedColor(this.defaultColorValue)
    }

    _applySelectedColor(color) {
        if (!color) {
            return
        }

        if (this.hasColorValueTarget) {
            this.colorValueTarget.value = color
        }

        if (this.hasColorPickerTarget) {
            this.colorPickerTarget.querySelectorAll(".color-option").forEach(button => {
                button.classList.remove("ring-offset-2", "ring-stone-400")
            })

            const selectedBtn = this.colorPickerTarget.querySelector(`.color-option[data-color="${color}"]`)
            if (selectedBtn) {
                selectedBtn.classList.add("ring-offset-2", "ring-stone-400")
            }
        }

        if (this.hasSelectedColorPreviewTarget) {
            this.selectedColorPreviewTarget.style.backgroundColor = color
        }
    }
}
