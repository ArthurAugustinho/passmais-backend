package com.passmais.domain.entity;

import com.passmais.domain.enums.PatientSex;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "patient_files", uniqueConstraints = {
        @UniqueConstraint(columnNames = "cpf")
})
public class PatientFile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "mother_name", length = 160)
    private String motherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", length = 20)
    private PatientSex sex;

    @Column(length = 160)
    private String email;

    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "full_address", length = 255)
    private String fullAddress;

    @Column(name = "has_legal_responsible")
    private Boolean hasLegalResponsible;

    @Column(name = "responsible_full_name", length = 160)
    private String responsibleFullName;

    @Column(name = "responsible_relationship", length = 80)
    private String responsibleRelationship;

    @Column(name = "responsible_cpf", length = 14)
    private String responsibleCpf;

    @Column(name = "responsible_phone", length = 30)
    private String responsiblePhone;

    @Column(name = "health_insurance_name", length = 120)
    private String healthInsuranceName;

    @Column(name = "presence_confirmed_at")
    private Instant presenceConfirmedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
