package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.service.DiaryFormService;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.domain.tag.dto.TagResponse;
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
    private final DiaryFormService diaryFormService;

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
        var formData = diaryFormService.getCreateForm(profileId);
        populateModel(model, formData);
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
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            var formData = diaryFormService.getFormOnError(profileId, request, null);
            populateModel(model, formData);
            return "diaries/form";
        }

        if (diaryService.findIdByDate(profileId, request.date()) != null) {
            bindingResult.rejectValue("date", "duplicate", "이미 해당 날짜에 일기가 존재합니다");
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            var formData = diaryFormService.getFormOnError(profileId, request, null);
            populateModel(model, formData);
            return "diaries/form";
        }

        Long id = diaryService.createDiary(profileId, request);
        return new RedirectView("/diaries/" + id, true, false, false); // 303 리다이렉트
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            Model model
    ) {
        var formData = diaryFormService.getEditForm(profileId, id);
        populateModel(model, formData);
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
            var formData = diaryFormService.getFormOnError(profileId, request, id);
            populateModel(model, formData);
            return "diaries/form";
        }

        var existingId = diaryService.findIdByDate(profileId, request.date());
        if (existingId != null && !existingId.equals(id)) {
            bindingResult.rejectValue("date", "duplicate", "이미 해당 날짜에 일기가 존재합니다");
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            var formData = diaryFormService.getFormOnError(profileId, request, id);
            populateModel(model, formData);
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

    private void populateModel(Model model, com.mindlog.domain.diary.dto.DiaryFormDTO formData) {
        model.addAttribute("diaryRequest", formData.diaryRequest());
        model.addAttribute("tags", formData.tags());
        model.addAttribute("diaryId", formData.diaryId());
    }
}