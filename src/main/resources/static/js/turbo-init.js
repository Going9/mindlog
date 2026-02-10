import * as Turbo from "@hotwired/turbo"

if (window.__mindlogTurboInitialized) {
    window.Turbo = Turbo
    console.log("[Mindlog] Turbo 재초기화 요청 감지 - 기존 인스턴스 재사용")
} else {
    window.__mindlogTurboInitialized = true;

    // ---------------------------------------------------------
    // Preline UI 관련 전역 에러 방지 패치
    // 역할: 일부 Preline 컴포넌트(Overlay, ComboBox 등)가 초기화되기 전에
    // resize 이벤트 등이 발생할 때 발생하는 'Cannot read properties of undefined (reading 'length')' 에러를 방지함.
    // ---------------------------------------------------------
    ;[
        '$hsAccordionCollection', '$hsCarouselCollection', '$hsCollapseCollection',
        '$hsComboBoxCollection', '$hsCopyMarkupCollection', '$hsDataTableCollection',
        '$hsDropdownCollection', '$hsFileUploadCollection', '$hsInputNumberCollection',
        '$hsLayoutSplitterCollection', '$hsOverlayCollection', '$hsPinInputCollection',
        '$hsRangeSliderCollection', '$hsRemoveElementCollection', '$hsScrollspyCollection',
        '$hsSelectCollection', '$hsStepperCollection', '$hsStrongPasswordCollection',
        '$hsTabsCollection', '$hsTextareaAutoHeightCollection', '$hsThemeSwitchCollection',
        '$hsToggleCountCollection', '$hsTogglePasswordCollection', '$hsTooltipCollection',
        '$hsTreeViewCollection'
    ].forEach(collection => {
        window[collection] = window[collection] || []
    })

    // 1. Turbo 시작 (가장 중요)
    Turbo.start()

    // 2. 디버깅을 위해 전역 변수에 등록
    window.Turbo = Turbo

    // ===== 전역 Turbo 로딩 피드백 상태 =====
    let currentLoadingElement = null

    function setLoadingState(element) {
        if (!element) return

        // 이전 로딩 상태 해제
        clearLoadingState()

        currentLoadingElement = element
        element.classList.add('turbo-loading')
        element.setAttribute('aria-busy', 'true')
    }

    function clearLoadingState() {
        if (currentLoadingElement) {
            currentLoadingElement.classList.remove('turbo-loading')
            currentLoadingElement.removeAttribute('aria-busy')
            currentLoadingElement = null
        }

        // 모든 로딩 상태 요소 해제 (안전장치)
        document.querySelectorAll('.turbo-loading').forEach(el => {
            el.classList.remove('turbo-loading')
            el.removeAttribute('aria-busy')
        })
    }

    // 3. 화면 로드 시 로깅 + Preline 재초기화
    document.addEventListener("turbo:load", () => {
        console.log("[Mindlog] Turbo 화면 로드 완료")

        if (typeof HSStaticMethods !== 'undefined') {
            HSStaticMethods.autoInit()
        }

        clearLoadingState()
    })

    // 4. 인증 만료 처리
    // 서버에서 401(Unauthorized)이나 403(Forbidden) 응답이 오면 로그인 페이지로
    document.addEventListener('turbo:before-fetch-response', (event) => {
        const response = event.detail.fetchResponse
        const status = response.status

        if ((status === 401 || status === 403) && window.location.pathname !== '/auth/login') {
            event.preventDefault() // Turbo의 에러 처리를 막고
            console.log("[Mindlog] 세션 만료 감지 -> 로그인 페이지 이동")
            window.location.href = '/auth/login?error=session_expired' // 강제 이동
        }
    })

    // 5. 커스텀 삭제 확인 모달 (Preline UI)
    // 역할: data-turbo-confirm 속성이 있는 요소 클릭 시 기본 confirm() 대신 Preline 모달을 띄움.
    // Native 앱 환경에서는 시스템 다이얼로그를 사용하도록 유지함.
    document.addEventListener("turbo:confirm", (event) => {
        // Turbo Native(Android/iOS) 환경에서는 네이티브 다이얼로그를 사용하도록 내버려둠
        if (document.body.classList.contains('is-native')) {
            return
        }

        // 웹 환경에서는 커스텀 Preline 모달 사용
        event.preventDefault()

        const message = event.detail.message
        const modalElement = document.querySelector('#turbo-confirm-modal')
        const messageElement = document.querySelector('#turbo-confirm-message')
        const confirmButton = document.querySelector('#turbo-confirm-button')

        if (modalElement && messageElement && confirmButton) {
            messageElement.textContent = message

            // 확인 버튼 리스너 설정 (이전 리스너 제거를 위해 복제 후 교체)
            const newConfirmButton = confirmButton.cloneNode(true)
            confirmButton.parentNode.replaceChild(newConfirmButton, confirmButton)

            newConfirmButton.addEventListener('click', () => {
                // Preline 모달 닫기 (HSOverlay가 있으면 사용, 없으면 직접 class 조작)
                if (window.HSOverlay) {
                    window.HSOverlay.close(modalElement)
                } else {
                    modalElement.classList.add('hidden')
                }

                // Turbo 작업 재개
                event.detail.resume()
            }, { once: true })

            // Preline 모달 열기
            if (window.HSOverlay) {
                window.HSOverlay.open(modalElement)
            } else {
                modalElement.classList.remove('hidden')
            }
        } else {
            // 모달 요소가 없으면 기본 confirm 사용 (최종 폴백)
            if (window.confirm(message)) {
                event.detail.resume()
            }
        }
    })

    // 6. Turbo 링크 클릭 시 즉시 로딩 상태 표시
    document.addEventListener('turbo:click', (event) => {
        const target = event.target.closest('a, button')
        if (target) {
            setLoadingState(target)
        }
    })

    // 7. 폼 제출 시 로딩 상태 표시
    document.addEventListener('turbo:submit-start', (event) => {
        const submitButton = event.target.querySelector('button[type="submit"], input[type="submit"]')
        if (submitButton) {
            setLoadingState(submitButton)
        }
    })

    // 8. 프레임 로드 완료 시 로딩 상태 해제
    document.addEventListener('turbo:frame-load', () => {
        clearLoadingState()
    })

    // 9. 네비게이션 실패 시 로딩 상태 해제
    document.addEventListener('turbo:visit', () => {
        // visit 시작 후 일정 시간 내 완료되지 않으면 해제 (타임아웃 안전장치)
        setTimeout(() => {
            if (currentLoadingElement) {
                console.log('[Mindlog] 로딩 타임아웃 - 상태 해제')
                clearLoadingState()
            }
        }, 10000) // 10초 타임아웃
    })

    // 10. Custom Tab으로 열리는 링크 처리 (data-turbo="false")
    // 역할: Turbo 없이 열리는 링크에도 클릭 피드백 제공
    document.addEventListener('click', (event) => {
        const link = event.target.closest('a[data-turbo="false"]')
        if (link) {
            setLoadingState(link)

            // Custom Tab에서 돌아올 때를 대비한 상태 복구
            // visibilitychange 이벤트로 앱 복귀 감지
            const handleVisibilityChange = () => {
                if (document.visibilityState === 'visible') {
                    // 앱으로 돌아왔을 때 로딩 상태 해제
                    setTimeout(() => {
                        clearLoadingState()
                    }, 300)
                    document.removeEventListener('visibilitychange', handleVisibilityChange)
                }
            }
            document.addEventListener('visibilitychange', handleVisibilityChange)

            // 5초 후 자동 해제 (폴백)
            setTimeout(() => {
                clearLoadingState()
                document.removeEventListener('visibilitychange', handleVisibilityChange)
            }, 5000)
        }
    })
}
