package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.global.security.CurrentProfileId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping
    public String listDiaries(@CurrentProfileId UUID profileId, Model model) {
        List<DiaryResponse> diaries = diaryService.getDiaries(profileId);
        model.addAttribute("diaries", diaries);
        return "diary/list";
    }

    @GetMapping("/new")
    public String newDiaryForm(Model model) {
        model.addAttribute("diaryRequest",
                new DiaryRequest(LocalDate.now(), null, null, null, null, null, null, null, null));
        return "diary/form";
    }

    @PostMapping
    public String createDiary(
            @CurrentProfileId UUID profileId,
            @ModelAttribute DiaryRequest request) {
        DiaryResponse response = diaryService.createDiary(profileId, request);
        return "redirect:/diaries/" + response.id();
    }

    @GetMapping("/{id}")
    public String viewDiary(@CurrentProfileId UUID profileId, @PathVariable Long id, Model model) {
        DiaryResponse diary = diaryService.getDiary(profileId, id);
        model.addAttribute("diary", diary);
        return "diary/view";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteDiary(@CurrentProfileId UUID profileId, @PathVariable Long id) {
        diaryService.deleteDiary(profileId, id);
    }
}
