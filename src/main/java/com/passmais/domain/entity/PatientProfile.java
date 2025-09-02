package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patient_profiles")
public class PatientProfile {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 14, nullable = false)
    private String cpf;

    private LocalDate birthDate;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String cellPhone;

    @Column(length = 30)
    private String communicationPreference;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Dependent> dependents = new HashSet<>();
}
