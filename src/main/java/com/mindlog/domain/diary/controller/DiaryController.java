package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryFormDTO;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryWriteAllowance;
import com.mindlog.domain.diary.service.DiaryFormService;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.domain.diary.service.DiaryWritePolicyService;
import com.mindlog.global.security.CurrentProfileId;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/diaries")
public class DiaryController {

  private static final String FORM_VIEW = "diaries/form";

  private final DiaryService diaryService;
  private final DiaryFormService diaryFormService;
  private final DiaryWritePolicyService diaryWritePolicyService;
  private final DiaryIndexPageComposer diaryIndexPageComposer;

  public DiaryController(
      DiaryService diaryService,
      DiaryFormService diaryFormService,
      DiaryWritePolicyService diaryWritePolicyService) {
    this.diaryService = diaryService;
    this.diaryFormService = diaryFormService;
    this.diaryWritePolicyService = diaryWritePolicyService;
    this.diaryIndexPageComposer = new DiaryIndexPageComposer(diaryService);
  }

  @GetMapping
  public String index(
      @CurrentProfileId UUID profileId,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false, defaultValue = "latest") String sort,
      @RequestParam(name = "_refresh", required = false) Long refreshToken,
      @RequestParam(name = "q", required = false) String keyword,
      @RequestParam(required = false, defaultValue = "0") Integer page,
      Model model) {
    var pageModel = diaryIndexPageComposer.compose(
        profileId,
        year,
        month,
        sort,
        refreshToken,
        keyword,
        page);
    model.addAllAttributes(pageModel.attributes());
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
    return FORM_VIEW;
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
      HttpServletResponse response,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      var formData = diaryFormService.getFormOnError(profileId, request, null);
      return renderUnprocessableForm(response, model, formData);
    }

    try {
      var id = diaryService.createDiary(profileId, request);
      return redirectToDiaryDetail(id, "diary-created", redirectAttributes);
    } catch (DataIntegrityViolationException e) {
      return renderWriteConflict(response, model, bindingResult, profileId, request, null);
    }
  }

  @GetMapping("/{id}/edit")
  public String editForm(
      @CurrentProfileId UUID profileId,
      @PathVariable Long id,
      Model model) {
    var formData = diaryFormService.getEditForm(profileId, id);
    populateModel(model, formData);
    return FORM_VIEW;
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
      HttpServletResponse response,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      var formData = diaryFormService.getFormOnError(profileId, request, id);
      return renderUnprocessableForm(response, model, formData);
    }

    try {
      diaryService.updateDiary(profileId, id, request);
      return redirectToDiaryDetail(id, "diary-updated", redirectAttributes);
    } catch (DataIntegrityViolationException e) {
      return renderWriteConflict(response, model, bindingResult, profileId, request, id);
    }
  }

  /**
   * [표준 준수] 일기 삭제
   * Turbo Drive는 DELETE 요청 후 303 리다이렉트를 받아야 페이지를 이동시킨다.
   */
  @DeleteMapping("/{id}")
  public RedirectView delete(
      @CurrentProfileId UUID profileId,
      @PathVariable Long id,
      RedirectAttributes redirectAttributes) {
    diaryService.deleteDiary(profileId, id);
    return redirectToDiaryList("diary-deleted", redirectAttributes);
  }

  // 작성 제한 정책 조회용 (Turbo와 무관하게 유지)
  @GetMapping("/write-allowance")
  @ResponseBody
  public ResponseEntity<DiaryWriteAllowance> writeAllowance(
      @CurrentProfileId UUID profileId,
      @RequestParam LocalDate date) {
    return ResponseEntity.ok(diaryWritePolicyService.getAllowance(profileId, date));
  }

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

  private String renderWriteConflict(
      HttpServletResponse response,
      Model model,
      BindingResult bindingResult,
      UUID profileId,
      DiaryRequest request,
      Long diaryId) {
    bindingResult.reject("writeConflict", "저장 중 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");
    var formData = diaryFormService.getFormOnError(profileId, request, diaryId);
    return renderUnprocessableForm(response, model, formData);
  }

  private RedirectView redirectToDiaryDetail(
      Long id,
      String noticeCode,
      RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("noticeCode", noticeCode);
    var redirectUrl = UriComponentsBuilder.fromPath("/diaries/{id}")
        .buildAndExpand(id)
        .toUriString();
    return new RedirectView(redirectUrl, true, false, false);
  }

  private RedirectView redirectToDiaryList(String noticeCode, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("noticeCode", noticeCode);
    // 삭제 직후 목록 복귀는 캐시 프리뷰를 피하고 서버 최신 목록을 즉시 반영한다.
    var redirectUrl = UriComponentsBuilder.fromPath("/diaries")
        .queryParam("_refresh", System.currentTimeMillis())
        .toUriString();
    return new RedirectView(redirectUrl, true, false, false);
  }
}
