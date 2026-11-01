package com.editshare.entity;

import java.time.LocalDateTime;

public enum ExpirationDuration {
    AFTER_ONE_VIEW,
    TEN_MINUTES,
    ONE_HOUR,
    ONE_DAY,
    ONE_WEEK;

    public LocalDateTime calculateExpiration(LocalDateTime from) {
        if (from == null) {
            from = LocalDateTime.now();
        }
        return switch (this) {
            case AFTER_ONE_VIEW -> from.plusDays(7); // 7-day safety TTL if never opened
            case TEN_MINUTES -> from.plusMinutes(10);
            case ONE_HOUR -> from.plusHours(1);
            case ONE_DAY -> from.plusDays(1);
            case ONE_WEEK -> from.plusWeeks(1);
        };
    }
}
