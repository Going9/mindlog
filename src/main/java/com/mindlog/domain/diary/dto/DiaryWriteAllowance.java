package com.mindlog.domain.diary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiaryWriteAllowance(
    LocalDate date,
    long usedCount,
    @Nullable Integer dailyLimit,
    @Nullable Integer remainingCount,
    boolean canWrite
) {
  public static DiaryWriteAllowance unlimited(LocalDate date, long usedCount) {
    return new DiaryWriteAllowance(date, usedCount, null, null, true);
  }
}
