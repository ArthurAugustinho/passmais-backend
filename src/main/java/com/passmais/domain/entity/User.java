package com.passmais.domain.entity;

import com.passmais.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email"})
})
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private Instant lgpdAcceptedAt;

    private Instant emailVerifiedAt;

    @Column(length = 64)
    private String verificationCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 14)
    private String cpf;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    private Instant accountLockedUntil;

    private Instant lastTokenRevalidatedAt;
}
