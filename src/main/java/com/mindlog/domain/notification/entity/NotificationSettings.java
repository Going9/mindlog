package com.mindlog.domain.notification.entity;

import com.mindlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_settings")
public class NotificationSettings extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "profile_id", nullable = false, unique = true)
    private UUID profileId;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    @Nullable
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "reminder_times", columnDefinition = "time[]")
    private List<LocalTime> reminderTimes;

    @Nullable
    @Column(name = "custom_message")
    private String customMessage;

    @Nullable
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "push_subscription", columnDefinition = "jsonb")
    private Map<String, Object> pushSubscription;

    @Builder
    public NotificationSettings(
            UUID profileId,
            boolean isEnabled,
            @Nullable List<LocalTime> reminderTimes,
            @Nullable String customMessage,
            @Nullable Map<String, Object> pushSubscription
    ) {
        this.profileId = profileId;
        this.isEnabled = isEnabled;
        this.reminderTimes = reminderTimes;
        this.customMessage = customMessage;
        this.pushSubscription = pushSubscription;
    }

    public void updateEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void updateReminderTimes(@Nullable List<LocalTime> reminderTimes) {
        this.reminderTimes = reminderTimes;
    }

    public void updateCustomMessage(@Nullable String customMessage) {
        this.customMessage = customMessage;
    }

    public void updatePushSubscription(@Nullable Map<String, Object> pushSubscription) {
        this.pushSubscription = pushSubscription;
    }
}
