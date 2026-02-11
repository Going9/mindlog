package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryListItemResponse;
import com.mindlog.domain.diary.dto.DiaryFormDTO;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.exception.DuplicateDiaryDateException;
import com.mindlog.domain.diary.service.DiaryFormService;
import com.mindlog.domain.diary.service.DiaryService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryController {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월");
    private static final String FORM_VIEW = "diaries/form";
    private static final int SEARCH_PAGE_SIZE = 12;

    private final DiaryService diaryService;
    private final DiaryFormService diaryFormService;

    @GetMapping
    public String index(
            @CurrentProfileId UUID profileId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            Model model) {
        var currentYearMonth = resolveYearMonth(year, month);
        var y = currentYearMonth.getYear();
        var m = currentYearMonth.getMonthValue();
        var previous = currentYearMonth.minusMonths(1);
        var next = currentYearMonth.plusMonths(1);
        var normalizedSort = "oldest".equalsIgnoreCase(sort) ? "oldest" : "latest";
        var newestFirst = "latest".equals(normalizedSort);
        var normalizedKeyword = normalizeKeyword(keyword);
        var normalizedPage = (page != null && page >= 0) ? page : 0;

        if (normalizedKeyword != null) {
            var searchResult = diaryService.searchDiaries(
                    profileId,
                    normalizedKeyword,
                    null,
                    null,
                    newestFirst,
                    normalizedPage,
                    SEARCH_PAGE_SIZE);

            model.addAttribute("diaries", searchResult.getContent());
            model.addAttribute("sort", normalizedSort);
            model.addAttribute("keyword", normalizedKeyword);
            model.addAttribute("page", normalizedPage);
            model.addAttribute("hasPrev", searchResult.hasPrevious());
            model.addAttribute("hasNext", searchResult.hasNext());
            model.addAttribute("prevPage", searchResult.hasPrevious() ? normalizedPage - 1 : 0);
            model.addAttribute("nextPage", searchResult.hasNext() ? normalizedPage + 1 : normalizedPage);
            model.addAttribute("totalPages", searchResult.getTotalPages());
            model.addAttribute("totalElements", searchResult.getTotalElements());
            model.addAttribute("pageSize", SEARCH_PAGE_SIZE);
            model.addAttribute("isSearchMode", true);
            model.addAttribute("year", y);
            model.addAttribute("month", m);
            model.addAttribute("monthLabel", currentYearMonth.format(MONTH_FORMATTER));
            model.addAttribute("prevYear", previous.getYear());
            model.addAttribute("prevMonth", previous.getMonthValue());
            model.addAttribute("nextYear", next.getYear());
            model.addAttribute("nextMonth", next.getMonthValue());
            model.addAttribute("yearOptions", diaryService.getAvailableYears(profileId, y));
            model.addAttribute("monthOptions", IntStream.rangeClosed(1, 12).boxed().toList());
            return "diaries/index";
        }

        List<DiaryListItemResponse> diaries = diaryService.getMonthlyDiaries(profileId, y, m, newestFirst);

        model.addAttribute("diaries", diaries);
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("monthLabel", currentYearMonth.format(MONTH_FORMATTER));
        model.addAttribute("prevYear", previous.getYear());
        model.addAttribute("prevMonth", previous.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        model.addAttribute("yearOptions", diaryService.getAvailableYears(profileId, y));
        model.addAttribute("monthOptions", IntStream.rangeClosed(1, 12).boxed().toList());
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("keyword", null);
        model.addAttribute("page", 0);
        model.addAttribute("hasPrev", false);
        model.addAttribute("hasNext", false);
        model.addAttribute("totalPages", 0);
        model.addAttribute("totalElements", 0L);
        model.addAttribute("isSearchMode", false);

        return "diaries/index";
    }

    @GetMapping("/{id}")
    public String detail(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            Model model) {
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
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            var formData = diaryFormService.getFormOnError(profileId, request, null);
            return renderUnprocessableForm(response, model, formData);
        }

        try {
            var id = diaryService.createDiary(profileId, request);
            return redirectToDiaryDetail(id, "diary-created");
        } catch (DuplicateDiaryDateException e) {
            bindingResult.rejectValue("date", "duplicate", e.getMessage());
            var formData = diaryFormService.getFormOnError(profileId, request, null);
            return renderUnprocessableForm(response, model, formData);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            Model model) {
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
            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            var formData = diaryFormService.getFormOnError(profileId, request, id);
            return renderUnprocessableForm(response, model, formData);
        }

        try {
            diaryService.updateDiary(profileId, id, request);
            return redirectToDiaryDetail(id, "diary-updated");
        } catch (DuplicateDiaryDateException e) {
            bindingResult.rejectValue("date", "duplicate", e.getMessage());
            var formData = diaryFormService.getFormOnError(profileId, request, id);
            return renderUnprocessableForm(response, model, formData);
        }
    }

    /**
     * [표준 준수] 일기 삭제
     * 기존: ResponseEntity (JSON) -> 변경: RedirectView (HTML 네비게이션)
     * Turbo Drive는 DELETE 요청 후 303 리다이렉트를 받아야 페이지를 이동시킴.
     */
    @DeleteMapping("/{id}")
    public RedirectView delete(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id) {
        diaryService.deleteDiary(profileId, id);
        return redirectToDiaryList("diary-deleted");
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

    private void populateModel(Model model, DiaryFormDTO formData) {
        model.addAttribute("diaryRequest", formData.diaryRequest());
        model.addAttribute("tags", formData.tags());
        model.addAttribute("diaryId", formData.diaryId());
    }

    private String renderUnprocessableForm(
            HttpServletResponse response,
            Model model,
            DiaryFormDTO formData) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        populateModel(model, formData);
        return FORM_VIEW;
    }

    private RedirectView redirectToDiaryDetail(Long id, String noticeCode) {
        var redirectUrl = UriComponentsBuilder.fromPath("/diaries/{id}")
                .queryParam("noticeCode", noticeCode)
                .buildAndExpand(id)
                .toUriString();
        return new RedirectView(redirectUrl, true, false, false);
    }

    private RedirectView redirectToDiaryList(String noticeCode) {
        var redirectUrl = UriComponentsBuilder.fromPath("/diaries")
                .queryParam("noticeCode", noticeCode)
                .toUriString();
        return new RedirectView(redirectUrl, true, false, false);
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        var now = LocalDate.now();
        var resolvedYear = (year != null) ? year : now.getYear();
        var resolvedMonth = (month != null) ? month : now.getMonthValue();

        try {
            return YearMonth.of(resolvedYear, resolvedMonth);
        } catch (RuntimeException ignored) {
            return YearMonth.of(now.getYear(), now.getMonthValue());
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        var trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
