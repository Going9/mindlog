package com.mindlog.domain.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.mindlog.domain.diary.dto.DiaryFormDTO;
import com.mindlog.domain.diary.dto.DiaryRequest;
import com.mindlog.domain.diary.exception.DuplicateDiaryDateException;
import com.mindlog.domain.diary.service.DiaryFormService;
import com.mindlog.domain.diary.service.DiaryService;
import com.mindlog.global.security.CurrentProfileId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

    private MockMvc mockMvc;
    private UUID profileId;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();

        var controller = new DiaryController(diaryService, diaryFormService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new FixedProfileIdResolver(profileId))
                .build();
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
                .andExpect(redirectedUrl("/diaries/10?noticeCode=diary-created"));
    }

    @Test
    void update_WhenDuplicateDate_Returns422() throws Exception {
        var formData = new DiaryFormDTO(
                new DiaryRequest(LocalDate.now(), null, null, null, null, null, null, null, null, List.of()),
                List.of(),
                10L);
        when(diaryFormService.getFormOnError(eq(profileId), any(DiaryRequest.class), eq(10L))).thenReturn(formData);
        doThrow(new DuplicateDiaryDateException("중복"))
                .when(diaryService)
                .updateDiary(eq(profileId), eq(10L), any(DiaryRequest.class));

        mockMvc.perform(put("/diaries/10")
                        .param("date", "2026-02-11"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(view().name("diaries/form"));
    }

    @Test
    void delete_WhenSuccess_Returns303() throws Exception {
        mockMvc.perform(delete("/diaries/10"))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl("/diaries?noticeCode=diary-deleted"));

        verify(diaryService).deleteDiary(profileId, 10L);
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
