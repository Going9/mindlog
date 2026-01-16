import { Application, Controller } from "https://unpkg.com/@hotwired/stimulus/dist/stimulus.js"

window.Stimulus = Application.start()

Stimulus.register("preline", class extends Controller {
    connect() {
        // 요소가 DOM에 연결될 때마다 Preline UI를 다시 깨웁니다.
        // Turbo 렌더링 안정화를 위해 잠시 대기
        setTimeout(() => {
            // Turbo 캐시와 충돌 방지를 위해 기존 상태 정리
            window.HSStaticMethods?.removeCollection?.();

            // 안전하게 초기화
            window.HSStaticMethods?.autoInit?.();
        }, 100);
    }
})
