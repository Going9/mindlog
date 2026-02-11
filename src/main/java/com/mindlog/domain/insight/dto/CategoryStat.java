package com.mindlog.domain.insight.dto;

import java.io.Serializable;

public record CategoryStat(
        String category,
        long count,
        double ratio
) implements Serializable {
}
