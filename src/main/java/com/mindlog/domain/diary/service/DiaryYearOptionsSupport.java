package com.mindlog.domain.diary.service;

import com.mindlog.domain.diary.repository.DiaryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

final class DiaryYearOptionsSupport {

  private static final long YEAR_OPTIONS_CACHE_TTL_MS = 5 * 60 * 1000;

  private final DiaryRepository diaryRepository;
  private final Map<UUID, YearOptionsCacheEntry> yearOptionsCache = new ConcurrentHashMap<>();

  DiaryYearOptionsSupport(DiaryRepository diaryRepository) {
    this.diaryRepository = diaryRepository;
  }

  List<Integer> getAvailableYears(UUID profileId, int selectedYear) {
    var cachedEntry = yearOptionsCache.get(profileId);
    if (cachedEntry != null && !cachedEntry.isExpired()) {
      return cachedEntry.years();
    }

    var dateRange = diaryRepository.findDateRangeByProfileId(profileId);
    LocalDate minDate = dateRange != null ? dateRange.getMinDate() : null;
    LocalDate maxDate = dateRange != null ? dateRange.getMaxDate() : null;

    if (minDate == null || maxDate == null) {
      var fallbackYears = IntStream.rangeClosed(selectedYear - 2, selectedYear + 2)
          .boxed()
          .sorted((a, b) -> Integer.compare(b, a))
          .toList();
      yearOptionsCache.put(profileId, YearOptionsCacheEntry.of(fallbackYears));
      return fallbackYears;
    }

    int startYear = Math.min(minDate.getYear(), selectedYear);
    int endYear = Math.max(maxDate.getYear(), selectedYear);

    var availableYears = IntStream.rangeClosed(startYear, endYear)
        .boxed()
        .sorted((a, b) -> Integer.compare(b, a))
        .toList();
    yearOptionsCache.put(profileId, YearOptionsCacheEntry.of(availableYears));
    return availableYears;
  }

  void invalidate(UUID profileId) {
    yearOptionsCache.remove(profileId);
  }

  private record YearOptionsCacheEntry(List<Integer> years, long expiresAtMs) {
    private static YearOptionsCacheEntry of(List<Integer> years) {
      return new YearOptionsCacheEntry(years, System.currentTimeMillis() + YEAR_OPTIONS_CACHE_TTL_MS);
    }

    private boolean isExpired() {
      return System.currentTimeMillis() > expiresAtMs;
    }
  }
}
