import * as Turbo from "@hotwired/turbo"

// ---------------------------------------------------------
// Preline UI 관련 전역 에러 방지 패치
// 역할: 일부 Preline 컴포넌트(Overlay, ComboBox 등)가 초기화되기 전에 
// resize 이벤트 등이 발생할 때 발생하는 'Cannot read properties of undefined (reading 'length')' 에러를 방지함.
// ---------------------------------------------------------
[
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
    window[collection] = window[collection] || [];
});

// 1. Turbo 시작 (가장 중요)
Turbo.start()

// 2. 디버깅을 위해 전역 변수에 등록
window.Turbo = Turbo

// 3. [수정됨] 수동 연결 로직 삭제
// // // 안드로이드 앱이 실행되면 자동으로 네이티브 어댑터가 연결
// // // 웹 코드에서 connect()를 강제로 호출할 필요 X

// 4. 로드 확인 로그
document.addEventListener("turbo:load", () => {
    console.log("[Mindlog] Turbo 화면 로드 완료")
    
    if (typeof HSStaticMethods !== 'undefined') {
        HSStaticMethods.autoInit();
    }
})

// 5. 인증 만료 처리
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

// 6. 커스텀 삭제 확인 모달 (Preline UI)
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