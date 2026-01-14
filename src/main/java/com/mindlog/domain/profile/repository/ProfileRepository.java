package com.mindlog.domain.profile.repository;

import com.mindlog.domain.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserName(String userName);

    Optional<Profile> findByEmail(String email);

    boolean existsByUserName(String userName);

    boolean existsByEmail(String email);
}
