package com.mindlog.domain.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import com.mindlog.domain.diary.dto.DiaryFormDTO;
import com.mindlog.domain.diary.dto.DiaryListItemResponse;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.dto.DiaryWriteAllowance;
import com.mindlog.domain.diary.service.DiaryFormService;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.domain.diary.service.DiaryWritePolicyService;
import com.mindlog.global.security.CurrentProfileId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class DiaryControllerWebMvcTest {

    @Mock
    private DiaryService diaryService;

    @Mock
    private DiaryFormService diaryFormService;

    @Mock
    private DiaryWritePolicyService diaryWritePolicyService;

    private MockMvc mockMvc;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();

        var controller = new DiaryController(diaryService, diaryFormService, diaryWritePolicyService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new FixedProfileIdResolver(profileId))
                .build();
    }

    @Test
    void index_WhenCalled_PassesMonthParamsToService() throws Exception {
        when(diaryService.getMonthlyDiaries(eq(profileId), eq(2026), eq(2), eq(true)))
                .thenReturn(List.<DiaryListItemResponse>of());
        when(diaryService.getAvailableYears(eq(profileId), eq(2026))).thenReturn(List.of(2026));

        mockMvc.perform(get("/diaries")
                        .param("year", "2026")
                        .param("month", "2")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(view().name("diaries/index"));

        verify(diaryService).getMonthlyDiaries(eq(profileId), eq(2026), eq(2), eq(true));
    }

    @Test
    void index_WhenRefreshTokenExists_BypassesCache() throws Exception {
        when(diaryService.getMonthlyDiariesFresh(eq(profileId), eq(2026), eq(2), eq(true)))
                .thenReturn(List.<DiaryListItemResponse>of());
        when(diaryService.getAvailableYears(eq(profileId), eq(2026))).thenReturn(List.of(2026));

        mockMvc.perform(get("/diaries")
                        .param("year", "2026")
                        .param("month", "2")
                        .param("sort", "latest")
                        .param("_refresh", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("diaries/index"));

        verify(diaryService).getMonthlyDiariesFresh(eq(profileId), eq(2026), eq(2), eq(true));
    }

    @Test
    void search_WhenCalled_PassesSearchParamsToService() throws Exception {
        when(diaryService.searchDiaries(
                eq(profileId), eq("행복"), eq(null), eq(null),
                eq(true), eq(0), eq(12)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/diaries")
                        .param("q", "행복")
                        .param("sort", "latest")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("diaries/index"));

        verify(diaryService).searchDiaries(
                eq(profileId), eq("행복"), eq(null), eq(null),
                eq(true), eq(0), eq(12));
    }

    @Test
    void create_WhenValidationFails_Returns422() throws Exception {
        var formData = new DiaryFormDTO(
                new DiaryRequest(LocalDate.now(), null, null, null, null, null, null, null, null, List.of()),
                List.of(),
                null);
        when(diaryFormService.getFormOnError(eq(profileId), any(DiaryRequest.class), eq(null))).thenReturn(formData);

        mockMvc.perform(post("/diaries"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("diaries/form"));

        verify(diaryFormService).getFormOnError(eq(profileId), any(DiaryRequest.class), eq(null));
    }

    @Test
    void create_WhenSuccess_Returns303() throws Exception {
        when(diaryService.createDiary(eq(profileId), any(DiaryRequest.class))).thenReturn(10L);

        mockMvc.perform(post("/diaries")
                        .param("date", "2026-02-11")
                        .param("shortContent", "test"))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/diaries/10"))
                .andExpect(flash().attribute("noticeCode", "diary-created"));
    }

    @Test
    void create_WhenWriteConflict_Returns422() throws Exception {
        var formData = new DiaryFormDTO(
                new DiaryRequest(LocalDate.of(2026, 2, 11), "test", null, null, null, null, null, null, null, List.of()),
                List.of(),
                null);
        when(diaryFormService.getFormOnError(eq(profileId), any(DiaryRequest.class), eq(null))).thenReturn(formData);
        doThrow(new DataIntegrityViolationException("conflict"))
                .when(diaryService)
                .createDiary(eq(profileId), any(DiaryRequest.class));

        mockMvc.perform(post("/diaries")
                        .param("date", "2026-02-11")
                        .param("shortContent", "test"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("diaries/form"));
    }

    @Test
    void update_WhenSuccess_Returns303() throws Exception {
        mockMvc.perform(put("/diaries/10")
                        .param("date", "2026-02-11")
                        .param("shortContent", "updated"))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/diaries/10"))
                .andExpect(flash().attribute("noticeCode", "diary-updated"));

        verify(diaryService).updateDiary(eq(profileId), eq(10L), any(DiaryRequest.class));
    }

    @Test
    void update_WhenWriteConflict_Returns422() throws Exception {
        var formData = new DiaryFormDTO(
                new DiaryRequest(LocalDate.of(2026, 2, 11), "updated", null, null, null, null, null, null, null, List.of()),
                List.of(),
                10L);
        when(diaryFormService.getFormOnError(eq(profileId), any(DiaryRequest.class), eq(10L))).thenReturn(formData);
        doThrow(new DataIntegrityViolationException("conflict"))
                .when(diaryService)
                .updateDiary(eq(profileId), eq(10L), any(DiaryRequest.class));

        mockMvc.perform(put("/diaries/10")
                        .param("date", "2026-02-11")
                        .param("shortContent", "updated"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("diaries/form"));
    }

    @Test
    void delete_WhenSuccess_Returns303() throws Exception {
        mockMvc.perform(delete("/diaries/10"))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrlPattern("/diaries?_refresh=*"))
                .andExpect(flash().attribute("noticeCode", "diary-deleted"));

        verify(diaryService).deleteDiary(profileId, 10L);
    }

    @Test
    void writeAllowance_WhenCalled_ReturnsUsageSnapshot() throws Exception {
        when(diaryWritePolicyService.getAllowance(eq(profileId), eq(LocalDate.of(2026, 2, 11))))
                .thenReturn(DiaryWriteAllowance.unlimited(LocalDate.of(2026, 2, 11), 2L));

        mockMvc.perform(get("/diaries/write-allowance")
                        .param("date", "2026-02-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-02-11"))
                .andExpect(jsonPath("$.usedCount").value(2))
                .andExpect(jsonPath("$.dailyLimit").doesNotExist())
                .andExpect(jsonPath("$.remainingCount").doesNotExist())
                .andExpect(jsonPath("$.canWrite").value(true));
    }

    private static final class FixedProfileIdResolver implements HandlerMethodArgumentResolver {
        private final UUID profileId;

        private FixedProfileIdResolver(UUID profileId) {
            this.profileId = profileId;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentProfileId.class)
                    && parameter.getParameterType().equals(UUID.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return profileId;
        }
    }
}
