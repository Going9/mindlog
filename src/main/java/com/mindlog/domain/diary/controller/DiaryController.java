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

  @PostMapping
  public String create(
      @CurrentProfileId UUID profileId,
      @ModelAttribute DiaryRequest request,
      RedirectAttributes redirectAttributes
  ) {
    Long id = diaryService.createDiary(profileId, request);
    return "redirect:/diaries/" + id;
  }

  @GetMapping("/{id}/edit")
  public String editForm(
      @CurrentProfileId UUID profileId,
      @PathVariable Long id,
      Model model
  ) {
    DiaryResponse diary = diaryService.getDiary(profileId, id);

    List<Long> existingTagIds = diary.tags().stream()
        .map(EmotionTag::getId)
        .toList();

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
    model.addAttribute("diaryId", id);

    // [수정] 여기서도 TagResponse로 변환해서 넘겨야 일관성이 유지됩니다.
    List<TagResponse> tags = tagService.getAllTags(profileId).stream()
        .map(TagResponse::from)
        .toList();
    model.addAttribute("tags", tags);

    return "diaries/edit";
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
    // 해당 날짜에 일기가 있으면 ID 반환, 없으면 null 반환
    // (Service에 findIdByDate 같은 메서드가 없으면 Optional<Diary>로 찾아서 getId 처리)
    Long diaryId = diaryService.findIdByDate(profileId, date);
    return ResponseEntity.ok(diaryId);
  }

  // [수정/통합] SSR 최적화가 적용된 폼 조회 메서드
  @GetMapping("/new")
  public String getForm(@CurrentProfileId UUID profileId, Model model) {
    // 1. 빈 폼 객체 (Record는 생성자로 초기화)
    model.addAttribute("diaryRequest", new DiaryRequest(
        LocalDate.now(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    ));

    // 2. 태그 목록 조회 및 DTO 변환 (TagService가 EmotionTag를 반환한다고 가정)
    List<TagResponse> tags = tagService.getAllTags(profileId).stream()
        .map(TagResponse::from) // EmotionTag -> TagResponse 변환
        .toList();
    model.addAttribute("tags", tags);

    // 3. 오늘 날짜 일기 존재 여부 체크
    Long todayDiaryId = diaryService.findIdByDate(profileId, LocalDate.now());
    model.addAttribute("existingDiaryId", todayDiaryId);

    return "diaries/form";
  }
}
