package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 30)
    private String crm;

    @Column(length = 80)
    private String specialty;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = false;

    private Instant approvedAt;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Availability> availabilities = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "doctor_clinics",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "clinic_id")
    )
    @Builder.Default
    private Set<Clinic> clinics = new HashSet<>();
}
