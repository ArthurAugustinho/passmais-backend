package com.passmais.interfaces.controller;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.ReviewRepository;
import com.passmais.interfaces.dto.DoctorPublicProfileDTO;
import com.passmais.interfaces.dto.PendingDoctorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorProfileRepository doctorRepo;
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorController(DoctorProfileRepository doctorRepo, ReviewRepository reviewRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepo = doctorRepo;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/pending")
    public ResponseEntity<java.util.List<PendingDoctorDTO>> listPending() {
        java.util.List<DoctorProfile> list = doctorRepo.findByApprovedAtIsNullAndRejectedAtIsNull();
        java.util.List<PendingDoctorDTO> resp = list.stream().map(d -> new PendingDoctorDTO(
                d.getId(),
                d.getUser().getName(),
                d.getSpecialty(),
                d.getCrm(),
                d.getCreatedAt(),
                d.getApprovedAt()
        )).toList();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/search")
    public ResponseEntity<List<DoctorPublicProfileDTO>> search(@RequestParam(name = "specialty", required = false) String specialty) {
        List<DoctorProfile> list = specialty == null || specialty.isBlank()
                ? doctorRepo.findAll()
                : doctorRepo.findBySpecialtyContainingIgnoreCase(specialty);
        List<DoctorPublicProfileDTO> dtos = list.stream()
                .filter(DoctorProfile::isApproved)
                .map(this::toPublic)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorPublicProfileDTO> getProfile(@PathVariable UUID id) {
        DoctorProfile d = doctorRepo.findById(id).orElseThrow();
        return ResponseEntity.ok(toPublic(d));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<com.passmais.domain.entity.Review>> listApprovedReviews(@PathVariable UUID id) {
        DoctorProfile d = doctorRepo.findById(id).orElseThrow();
        List<com.passmais.domain.entity.Review> list = reviewRepository.findAll().stream()
                .filter(r -> r.getAppointment().getDoctor().getId().equals(d.getId()))
                .filter(r -> r.getStatus() == com.passmais.domain.enums.ReviewStatus.APPROVED)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping("/{id}")
    public ResponseEntity<DoctorPublicProfileDTO> updateProfile(@PathVariable UUID id, @RequestBody DoctorPublicProfileDTO dto) {
        DoctorProfile d = doctorRepo.findById(id).orElseThrow();
        d.setBio(dto.bio());
        d.setSpecialty(dto.specialty());
        d.setCrm(dto.crm());
        d.setConsultationPrice(dto.consultationPrice());
        DoctorProfile saved = doctorRepo.save(d);
        return ResponseEntity.ok(toPublic(saved));
    }

    private DoctorPublicProfileDTO toPublic(DoctorProfile d) {
        Double avg = reviewRepository.averageForDoctor(d).orElse(null);
        long count = reviewRepository.countApprovedForDoctor(d);
        return new DoctorPublicProfileDTO(
                d.getId(),
                d.getUser().getName(),
                d.getCrm(),
                d.getSpecialty(),
                d.getBio(),
                d.getPhotoUrl(),
                d.getConsultationPrice(),
                avg,
                count
        );
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/{doctorId}/patients")
    public ResponseEntity<List<com.passmais.domain.entity.PatientProfile>> listPatients(
            @PathVariable UUID doctorId,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "cpf", required = false) String cpf,
            @RequestParam(name = "date", required = false) String date) {
        DoctorProfile doctor = doctorRepo.findById(doctorId).orElseThrow();
        List<com.passmais.domain.entity.PatientProfile> patients = appointmentRepository
                .findByDoctor(doctor)
                .stream()
                .map(a -> a.getPatient())
                .distinct()
                .filter(p -> name == null || p.getUser().getName().toLowerCase().contains(name.toLowerCase()))
                .filter(p -> cpf == null || (p.getCpf() != null && p.getCpf().equalsIgnoreCase(cpf)))
                .filter(p -> {
                    if (date == null) return true;
                    var d = LocalDate.parse(date);
                    var start = d.atStartOfDay().toInstant(ZoneOffset.UTC);
                    var end = d.atTime(java.time.LocalTime.MAX).toInstant(ZoneOffset.UTC);
                    return appointmentRepository.countByPatientAndDateTimeBetween(p, start, end) > 0;
                })
                .toList();
        return ResponseEntity.ok(patients);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{id}/photo")
    public ResponseEntity<DoctorPublicProfileDTO> uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws java.io.IOException {
        DoctorProfile d = doctorRepo.findById(id).orElseThrow();
        java.nio.file.Path dir = java.nio.file.Paths.get("uploads");
        java.nio.file.Files.createDirectories(dir);
        String filename = id + "-" + java.util.UUID.randomUUID() + "-" + file.getOriginalFilename();
        java.nio.file.Path path = dir.resolve(filename);
        file.transferTo(path.toFile());
        d.setPhotoUrl("/uploads/" + filename);
        DoctorProfile saved = doctorRepo.save(d);
        return ResponseEntity.ok(toPublic(saved));
    }
}
