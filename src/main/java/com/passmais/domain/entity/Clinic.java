package com.passmais.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "clinics")
public class Clinic {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 18)
    private String cnpj;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = false;

    private Instant approvedAt;

    @ManyToMany(mappedBy = "clinics")
    @Builder.Default
    private Set<DoctorProfile> doctors = new HashSet<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
