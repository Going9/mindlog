package com.mindlog.domain.user.repository;

import com.mindlog.domain.user.entity.AuthProvider;
import com.mindlog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<User> findByEmail(String email);

    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);
}
