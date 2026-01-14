package com.mindlog.domain.notification.repository;

import com.mindlog.domain.notification.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByProfileId(UUID profileId);

    boolean existsByProfileId(UUID profileId);

    List<NotificationSettings> findByIsEnabledTrue();
}
