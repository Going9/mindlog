package com.mindlog.domain.insight.controller;

import com.mindlog.domain.insight.service.EmotionInsightService;
import com.mindlog.global.security.CurrentProfileId;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/insights")
@RequiredArgsConstructor
public class EmotionInsightPageController {

    private final EmotionInsightService emotionInsightService;

    @GetMapping("/emotions")
    public String emotionInsightPage(
            @CurrentProfileId UUID profileId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @RequestParam(required = false) @Nullable Integer topN,
            Model model
    ) {
        try {
            var analysis = emotionInsightService.getEmotionAnalysis(profileId, from, to, topN);
            model.addAttribute("analysis", analysis);
            model.addAttribute("from", analysis.fromDate());
            model.addAttribute("to", analysis.toDate());
            model.addAttribute("topN", (topN == null) ? 10 : topN);
            return "insights/emotions";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
        }
    }
}
