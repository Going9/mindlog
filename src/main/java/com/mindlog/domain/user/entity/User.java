package com.mindlog.domain.user.entity;

import com.mindlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Nullable
    @Column(name = "email")
    private String email;

    @Nullable
    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Builder
    public User(UUID id, @Nullable String email, @Nullable String name, AuthProvider provider, String providerId) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
    }
}
