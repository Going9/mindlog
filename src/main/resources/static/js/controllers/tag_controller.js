import { Controller } from "@hotwired/stimulus"

/**
 * 태그 관리 Stimulus Controller
 * 
 * 태그 선택/해제, 모달 열기/닫기, 태그 생성 API 호출을 관리합니다.
 * 
 * 사용법:
 * <div data-controller="tag"
 *      data-tag-colors-value='["#EF4444", "#F97316", ...]'
 *      data-tag-api-endpoint-value="/api/tags">
 *   
 *   <div data-tag-target="selectedContainer"></div>
 *   <div data-tag-target="tagList">
 *     <button type="button" 
 *             data-action="click->tag#toggle" 
 *             data-tag-id="1" 
 *             data-tag-color="#EF4444">태그명</button>
 *   </div>
 *   <button data-action="click->tag#openModal">+ 태그 추가</button>
 *   
 *   <!-- Modal -->
 *   <div data-tag-target="modal" class="hidden">
 *     <input data-tag-target="tagName">
 *     <select data-tag-target="tagCategory">...</select>
 *     <div data-tag-target="colorPicker"></div>
 *     <input type="hidden" data-tag-target="colorValue">
 *     <div data-tag-target="errorMessage"></div>
 *     <button data-tag-target="submitBtn" data-action="click->tag#createTag">
 *       <svg data-tag-target="spinner">...</svg>
 *       <span data-tag-target="submitText">추가하기</span>
 *     </button>
 *   </div>
 * </div>
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
        "errorMessage",
        "submitBtn",
        "spinner",
        "submitText"
    ]

    static values = {
        colors: {
            type: Array,
            default: [
                '#EF4444', '#F97316', '#F59E0B', '#10B981', '#06B6D4',
                '#3B82F6', '#6366F1', '#8B5CF6', '#EC4899', '#78716C'
            ]
        },
        apiEndpoint: { type: String, default: '/api/tags' },
        loadingText: { type: String, default: '추가 중...' },
        defaultCategory: { type: String, default: 'POSITIVE' },
        defaultColor: { type: String, default: '#78716C' }
    }

    connect() {
        // 이미 선택된 태그가 있다면(hidden inputs), 버튼의 시각적 상태(Ring, Border)를 동기화합니다.
        // 이는 수정 페이지(edit.html) 등에서 서버가 렌더링한 초기 상태를 반영하기 위함입니다.
        if (this.hasSelectedContainerTarget && this.hasTagListTarget) {
            const inputs = this.selectedContainerTarget.querySelectorAll('input[name="tagIds"]')
            
            inputs.forEach(input => {
                const id = input.value
                const btn = this.tagListTarget.querySelector(`button[data-tag-id="${id}"]`)
                
                if (btn) {
                    const color = btn.dataset.tagColor
                    btn.classList.add('ring-2', 'ring-offset-1')
                    btn.style.borderColor = color
                    btn.style.boxShadow = `0 0 0 2px ${color}`
                }
            })
        }
    }

    /**
     * 태그 선택/해제 토글
     * data-tag-id, data-tag-color 속성 필요
     */
    toggle(event) {
        const btn = event.currentTarget
        const id = btn.dataset.tagId
        const color = btn.dataset.tagColor

        if (!this.hasSelectedContainerTarget) return

        const container = this.selectedContainerTarget
        let input = container.querySelector(`input[value="${id}"]`)

        if (input) {
            // 선택 해제
            input.remove()
            btn.classList.remove('ring-2', 'ring-offset-1')
            btn.style.boxShadow = ''
        } else {
            // 선택
            input = document.createElement('input')
            input.type = 'hidden'
            input.name = 'tagIds'
            input.value = id
            container.appendChild(input)

            btn.classList.add('ring-2', 'ring-offset-1')
            btn.style.borderColor = color
            btn.style.boxShadow = `0 0 0 2px ${color}`
        }
    }

    /**
     * 모달 열기
     */
    openModal() {
        if (!this.hasModalTarget) return

        this.modalTarget.classList.remove('hidden')
        this._clearError()
        this._initColorPicker()
    }

    /**
     * 모달 닫기
     */
    closeModal() {
        if (!this.hasModalTarget) return

        this.modalTarget.classList.add('hidden')
        this._resetForm()
    }

    /**
     * 색상 선택
     */
    selectColor(event) {
        const btn = event.currentTarget
        const color = btn.dataset.color

        if (this.hasColorValueTarget) {
            this.colorValueTarget.value = color
        }

        // 기존 선택 해제
        this.colorPickerTarget.querySelectorAll('.color-option').forEach(b => {
            b.classList.remove('ring-offset-2', 'ring-stone-400')
        })

        // 새 선택 표시
        btn.classList.add('ring-offset-2', 'ring-stone-400')
    }

    /**
     * 태그 생성 API 호출
     */
    async createTag() {
        const name = this.hasTagNameTarget ? this.tagNameTarget.value.trim() : ''
        const category = this.hasTagCategoryTarget ? this.tagCategoryTarget.value : this.defaultCategoryValue
        const color = this.hasColorValueTarget && this.colorValueTarget.value
            ? this.colorValueTarget.value
            : this.defaultColorValue

        if (!name) {
            this._showError('이름을 입력해주세요.')
            return
        }

        this._setLoading(true)

        try {
            const headers = { 'Content-Type': 'application/json' }
            this._addCsrfHeaders(headers)

            const response = await fetch(this.apiEndpointValue, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ name, color, category })
            })

            if (response.ok) {
                const newTag = await response.json()
                this._setLoading(false)
                this.closeModal()
                this._addTagToList(newTag)
            } else {
                const errorText = await response.text()
                this._showError(errorText || '태그 생성 실패')
                this._setLoading(false)
            }
        } catch (e) {
            console.error(e)
            this._showError('오류가 발생했습니다: ' + e.message)
            this._setLoading(false)
        }
    }

    // ========== Private Methods ==========

    /**
     * 색상 피커 초기화
     */
    _initColorPicker() {
        if (!this.hasColorPickerTarget) return
        if (this.colorPickerTarget.children.length > 0) return

        this.colorsValue.forEach(color => {
            const btn = document.createElement('button')
            btn.type = 'button'
            btn.className = 'w-10 h-10 rounded-full border-2 border-white shadow-sm cursor-pointer transition-transform hover:scale-110 color-option ring-2 ring-transparent hover:ring-gray-300'
            btn.style.backgroundColor = color
            btn.dataset.color = color
            btn.dataset.action = 'click->tag#selectColor'
            btn.setAttribute('data-action', 'click->tag#selectColor')
            this.colorPickerTarget.appendChild(btn)
        })
    }

    /**
     * 새 태그를 목록에 추가
     */
    _addTagToList(tag) {
        if (!this.hasTagListTarget) return

        const tagBtn = document.createElement('button')
        tagBtn.type = 'button'
        tagBtn.innerText = tag.name
        tagBtn.className = 'tag-btn px-3 py-1.5 rounded-full text-sm font-medium border transition-all duration-200 select-none bg-white shadow-sm'
        tagBtn.dataset.tagId = tag.id
        tagBtn.dataset.tagColor = tag.color
        tagBtn.dataset.action = 'click->tag#toggle'
        tagBtn.style.borderColor = tag.color
        tagBtn.style.color = tag.color

        // "태그 추가" 버튼 앞에 삽입
        const addButton = this.tagListTarget.lastElementChild
        this.tagListTarget.insertBefore(tagBtn, addButton)

        // 새 태그 자동 선택
        this._selectNewTag(tag, tagBtn)
    }

    /**
     * 새로 생성된 태그 자동 선택
     */
    _selectNewTag(tag, btn) {
        if (!this.hasSelectedContainerTarget) return

        const input = document.createElement('input')
        input.type = 'hidden'
        input.name = 'tagIds'
        input.value = tag.id
        this.selectedContainerTarget.appendChild(input)

        btn.classList.add('ring-2', 'ring-offset-1')
        btn.style.borderColor = tag.color
        btn.style.boxShadow = `0 0 0 2px ${tag.color}`
    }

    /**
     * CSRF 토큰 헤더 추가
     */
    _addCsrfHeaders(headers) {
        const csrfTokenMeta = document.querySelector('meta[name="_csrf"]')
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]')

        if (csrfTokenMeta && csrfHeaderMeta) {
            headers[csrfHeaderMeta.content] = csrfTokenMeta.content
        }
    }

    /**
     * 로딩 상태 설정
     */
    _setLoading(isLoading) {
        if (this.hasSubmitBtnTarget) {
            this.submitBtnTarget.disabled = isLoading
        }

        if (this.hasSpinnerTarget) {
            if (isLoading) {
                this.spinnerTarget.classList.remove('hidden')
            } else {
                this.spinnerTarget.classList.add('hidden')
            }
        }

        if (this.hasSubmitTextTarget) {
            this.submitTextTarget.textContent = isLoading ? this.loadingTextValue : '추가하기'
        }
    }

    /**
     * 에러 메시지 표시
     */
    _showError(message) {
        if (!this.hasErrorMessageTarget) return

        this.errorMessageTarget.innerText = message
        this.errorMessageTarget.classList.remove('hidden')
    }

    /**
     * 에러 메시지 숨김
     */
    _clearError() {
        if (!this.hasErrorMessageTarget) return

        this.errorMessageTarget.innerText = ''
        this.errorMessageTarget.classList.add('hidden')
    }

    /**
     * 폼 초기화
     */
    _resetForm() {
        if (this.hasTagNameTarget) {
            this.tagNameTarget.value = ''
        }

        if (this.hasTagCategoryTarget) {
            this.tagCategoryTarget.value = this.defaultCategoryValue
        }

        if (this.hasColorValueTarget) {
            this.colorValueTarget.value = ''
        }

        // 색상 선택 해제
        if (this.hasColorPickerTarget) {
            const selected = this.colorPickerTarget.querySelector('.color-option.ring-offset-2')
            if (selected) {
                selected.classList.remove('ring-offset-2', 'ring-stone-400')
            }
        }
    }
}
