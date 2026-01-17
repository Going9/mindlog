package com.mindlog.domain.diary.controller;

import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryResponse;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.global.security.CurrentProfileId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/diaries")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

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

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("date", LocalDate.now());
        model.addAttribute("diaryRequest", new DiaryRequest(
            LocalDate.now(), null, null, null, null, null, null, null, null
        ));
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

    @GetMapping("/{id}/edit")
    public String editForm(
            @CurrentProfileId UUID profileId,
            @PathVariable Long id,
            Model model
    ) {
        DiaryResponse diary = diaryService.getDiary(profileId, id);
        // Map response to request for the form
        DiaryRequest request = new DiaryRequest(
            diary.date(),
            diary.shortContent(),
            diary.situation(),
            diary.reaction(),
            diary.physicalSensation(),
            diary.desiredReaction(),
            diary.gratitudeMoment(),
            diary.selfKindWords(),
            diary.imageUrl()
        );
        model.addAttribute("diaryRequest", request);
        model.addAttribute("diaryId", id); // For update URL
        return "diaries/edit";
    }

    @PostMapping("/{id}/update") 
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
}
