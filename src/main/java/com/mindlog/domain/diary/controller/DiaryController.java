package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.domain.tag.dto.TagResponse;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.service.TagService;
import com.mindlog.global.security.CurrentProfileId;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final TagService tagService;

    @GetMapping
    public String index(
            @CurrentProfileId UUID profileId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model
    ) {
        var now = LocalDate.now();
        var y = (year != null) ? year : now.getYear();
        var m = (month != null) ? month : now.getMonthValue();

        List<DiaryResponse> diaries = diaryService.getMonthlyDiaries(profileId, y, m);

        model.addAttribute("diaries", diaries);
        model.addAttribute("year", y);
        model.addAttribute("month", m);

        return "diaries/index";
    }

    @GetMapping("/{id}")
    public String detail(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            Model model
    ) {
        var diary = diaryService.getDiary(profileId, id);
        model.addAttribute("diary", diary);
        return "diaries/detail";
    }

    @GetMapping("/new")
    public String getForm(@CurrentProfileId UUID profileId, Model model) {
        populateFormModel(model, profileId, new DiaryRequest(
                LocalDate.now(), null, null, null, null, null, null, null, null, null
        ), null);

        return "diaries/form";
    }

    /**
     * [표준 준수] 일기 생성
     * 성공: HTTP 303 Redirect -> 상세 페이지
     * 실패: HTTP 422 Unprocessable Entity -> 폼 다시 렌더링
     */
    @PostMapping
    public Object create(
            @CurrentProfileId UUID profileId,
            @Valid @ModelAttribute DiaryRequest request,
            BindingResult bindingResult,                 // 에러 결과 담는 통
            Model model,
            HttpServletResponse response
    ) {
        // 1. 입력값 검증 실패 시 (DB 가기 전에 컷)
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value()); // 422 (Turbo가 인식함)
            populateFormModel(model, profileId, request, null); // 폼 데이터 유지용
            return "diaries/form"; // 에러 메시지와 함께 폼 다시 띄움
        }

        // 2. 성공 시
        Long id = diaryService.createDiary(profileId, request);
        return new RedirectView("/diaries/" + id, true, false, false); // 303 리다이렉트
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            Model model
    ) {
        var diary = diaryService.getDiary(profileId, id);
        var existingTagIds = diary.tags().stream().map(EmotionTag::getId).toList();

        var request = new DiaryRequest(
                diary.date(), diary.shortContent(), diary.situation(), diary.reaction(),
                diary.physicalSensation(), diary.desiredReaction(), diary.gratitudeMoment(),
                diary.selfKindWords(), diary.imageUrl(), existingTagIds
        );

        populateFormModel(model, profileId, request, id);
        return "diaries/form";
    }

    /**
     * [표준 준수] 일기 수정
     * 성공: HTTP 303 Redirect
     * 실패: HTTP 422 Unprocessable Entity
     */
    @PutMapping("/{id}")
    public Object update(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            @Valid @ModelAttribute DiaryRequest request,
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            populateFormModel(model, profileId, request, id);
            return "diaries/form";
        }

        diaryService.updateDiary(profileId, id, request);
        return new RedirectView("/diaries/" + id, true, false, false);
    }

    /**
     * [표준 준수] 일기 삭제
     * 기존: ResponseEntity (JSON) -> 변경: RedirectView (HTML 네비게이션)
     * Turbo Drive는 DELETE 요청 후 303 리다이렉트를 받아야 페이지를 이동시킴.
     */
    @DeleteMapping("/{id}")
    public RedirectView delete(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id
    ) {
        diaryService.deleteDiary(profileId, id);
        // 삭제 후 목록으로 303 리다이렉트
        return new RedirectView("/diaries", true, false, false);
    }

    // Ajax 검증용 (Turbo와 무관하게 유지)
    @GetMapping("/check")
    @ResponseBody
    public ResponseEntity<Long> checkDiaryDate(@CurrentProfileId UUID profileId,
                                               @RequestParam LocalDate date) {
        var diaryId = diaryService.findIdByDate(profileId, date);
        return ResponseEntity.ok(diaryId);
    }

    // --- Private Helper Methods ---

    /**
     * 폼 렌더링에 필요한 공통 모델 데이터 주입
     * (유효성 검사 실패 시에도 동일한 데이터를 다시 로드해야 하므로 분리)
     */
    private void populateFormModel(Model model, UUID profileId, DiaryRequest request, Long diaryId) {
        var tags = tagService.getAllTags(profileId).stream()
                .map(TagResponse::from)
                .toList();

        model.addAttribute("diaryRequest", request);
        model.addAttribute("tags", tags);
        model.addAttribute("diaryId", diaryId); // null이면 생성, 값이 있으면 수정 모드
    }
}