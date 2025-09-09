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
        return doctorRepo.save(d);
    }

    public Clinic approveClinic(UUID clinicId) {
        Clinic c = clinicRepo.findById(clinicId).orElseThrow();
        c.setApproved(true);
        c.setApprovedAt(Instant.now());
        return clinicRepo.save(c);
    }

    public DoctorProfile rejectDoctor(UUID doctorId) {
        DoctorProfile d = doctorRepo.findById(doctorId).orElseThrow();
        d.setApproved(false);
        d.setApprovedAt(null);
        return doctorRepo.save(d);
    }

    public Clinic rejectClinic(UUID clinicId) {
        Clinic c = clinicRepo.findById(clinicId).orElseThrow();
        c.setApproved(false);
        c.setApprovedAt(null);
        return clinicRepo.save(c);
    }
}
