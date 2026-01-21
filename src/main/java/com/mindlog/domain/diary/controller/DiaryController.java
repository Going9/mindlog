package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.domain.tag.dto.TagResponse;
import com.mindlog.domain.tag.entity.EmotionTag;
import com.mindlog.domain.tag.service.TagService;
import com.mindlog.global.security.CurrentProfileId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    LocalDate now = LocalDate.now();
    int y = (year != null) ? year : now.getYear();
    int m = (month != null) ? month : now.getMonthValue();

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
    DiaryResponse diary = diaryService.getDiary(profileId, id);
    model.addAttribute("diary", diary);
    return "diaries/detail";
  }

  // [통합] 새 일기 폼
  @GetMapping("/new")
  public String getForm(@CurrentProfileId UUID profileId, Model model) {
    // 1. 빈 요청 객체 (ID 없음)
    model.addAttribute("diaryRequest", new DiaryRequest(
        LocalDate.now(),
        null, null, null, null, null, null, null, null, null
    ));

    // 2. 태그 목록
    List<TagResponse> tags = tagService.getAllTags(profileId).stream()
        .map(TagResponse::from)
        .toList();
    model.addAttribute("tags", tags);

    // 3. diaryId는 null로 두어 form.html에서 생성 모드임을 알림
    model.addAttribute("diaryId", null);

    return "diaries/form";
  }

  @PostMapping
  public String create(
      @CurrentProfileId UUID profileId,
      @ModelAttribute DiaryRequest request,
      RedirectAttributes redirectAttributes
  ) {
    Long id = diaryService.createDiary(profileId, request);
    return "redirect:/diaries/" + id;
  }

  // [통합] 일기 수정 폼 (RESTful URL 유지)
  @GetMapping("/{id}/edit")
  public String editForm(
      @CurrentProfileId UUID profileId,
      @PathVariable Long id,
      Model model
  ) {
    DiaryResponse diary = diaryService.getDiary(profileId, id);

    // 1. 기존 태그 ID 추출
    List<Long> existingTagIds = diary.tags().stream()
        .map(EmotionTag::getId)
        .toList();

    // 2. Response -> Request 변환 (폼 바인딩용)
    DiaryRequest request = new DiaryRequest(
        diary.date(),
        diary.shortContent(),
        diary.situation(),
        diary.reaction(),
        diary.physicalSensation(),
        diary.desiredReaction(),
        diary.gratitudeMoment(),
        diary.selfKindWords(),
        diary.imageUrl(),
        existingTagIds
    );

    model.addAttribute("diaryRequest", request);

    // 3. 수정 모드임을 알리기 위해 ID 전달
    model.addAttribute("diaryId", id);

    // 4. 태그 목록
    List<TagResponse> tags = tagService.getAllTags(profileId).stream()
        .map(TagResponse::from)
        .toList();
    model.addAttribute("tags", tags);

    // 5. form.html 재사용
    return "diaries/form";
  }

  @PutMapping("/{id}")
  public String update(
      @CurrentProfileId UUID profileId,
      @PathVariable Long id,
      @ModelAttribute DiaryRequest request
  ) {
    diaryService.updateDiary(profileId, id, request);
    return "redirect:/diaries/" + id;
  }

  @DeleteMapping("/{id}")
  @ResponseBody
  public ResponseEntity<Void> delete(
      @CurrentProfileId UUID profileId,
      @PathVariable Long id
  ) {
    diaryService.deleteDiary(profileId, id);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/check")
  @ResponseBody
  public ResponseEntity<Long> checkDiaryDate(@CurrentProfileId UUID profileId,
      @RequestParam LocalDate date) {
    Long diaryId = diaryService.findIdByDate(profileId, date);
    return ResponseEntity.ok(diaryId);
  }
}