package com.passmais.application.service;

import com.passmais.domain.entity.Clinic;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.infrastructure.repository.ClinicRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminApprovalService {

    private final DoctorProfileRepository doctorRepo;
    private final ClinicRepository clinicRepo;

    public AdminApprovalService(DoctorProfileRepository doctorRepo, ClinicRepository clinicRepo) {
        this.doctorRepo = doctorRepo;
        this.clinicRepo = clinicRepo;
    }

    public DoctorProfile approveDoctor(UUID doctorId) {
        DoctorProfile d = doctorRepo.findById(doctorId).orElseThrow();
        d.setApproved(true);
        d.setApprovedAt(Instant.now());
        d.setRejectedAt(null);
        d.setFailureDescription(null);
        return doctorRepo.save(d);
    }

    public Clinic approveClinic(UUID clinicId) {
        Clinic c = clinicRepo.findById(clinicId).orElseThrow();
        c.setApproved(true);
        c.setApprovedAt(Instant.now());
        return clinicRepo.save(c);
    }

    public DoctorProfile rejectDoctor(UUID doctorId, String failureDescription) {
        DoctorProfile doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Doctor not found"));
        Instant rejectionTime = Instant.now();
        doctor.setApproved(false);
        doctor.setApprovedAt(null);
        doctor.setRejectedAt(rejectionTime);
        String sanitizedReason = (failureDescription == null || failureDescription.isBlank()) ? null : failureDescription.trim();
        doctor.setFailureDescription(sanitizedReason);
        return doctorRepo.save(doctor);
    }

    public Clinic rejectClinic(UUID clinicId) {
        Clinic clinic = clinicRepo.findById(clinicId).orElseThrow();
        Instant rejectionTime = Instant.now();
        clinic.setApproved(false);
        clinic.setApprovedAt(rejectionTime);
        return clinicRepo.save(clinic);
    }
}
