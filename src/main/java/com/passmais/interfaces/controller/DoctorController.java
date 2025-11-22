package com.passmais.interfaces.controller;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import com.passmais.infrastructure.repository.ReviewRepository;
import com.passmais.interfaces.dto.DoctorPublicProfileDTO;
import com.passmais.interfaces.dto.PendingDoctorDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorProfileRepository doctorRepo;
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;

    public DoctorController(DoctorProfileRepository doctorRepo,
                            ReviewRepository reviewRepository,
                            AppointmentRepository appointmentRepository,
                            PatientProfileRepository patientProfileRepository) {
        this.doctorRepo = doctorRepo;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientProfileRepository = patientProfileRepository;
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
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DoctorPublicProfileDTO> updateProfile(@PathVariable UUID id, @RequestBody DoctorPublicProfileDTO dto) throws java.io.IOException {
        DoctorProfile updated = updateDoctorProfile(id, dto, null);
        return ResponseEntity.ok(toPublic(updated));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DoctorPublicProfileDTO> updateProfileWithPhoto(@PathVariable UUID id,
                                                                         @RequestPart("doctor") DoctorPublicProfileDTO dto,
                                                                         @RequestPart(value = "photo", required = false) MultipartFile photo) throws java.io.IOException {
        DoctorProfile updated = updateDoctorProfile(id, dto, photo);
        return ResponseEntity.ok(toPublic(updated));
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
                d.getClinicName(),
                d.getClinicStreetAndNumber(),
                d.getClinicCity(),
                d.getClinicPostalCode(),
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
        List<com.passmais.domain.entity.Appointment> appointments = appointmentRepository.findByDoctor(doctor);
        Map<UUID, List<com.passmais.domain.entity.Appointment>> appointmentsByUser = appointments.stream()
                .filter(a -> a.getPatient() != null)
                .collect(Collectors.groupingBy(a -> a.getPatient().getId()));
        if (appointmentsByUser.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<PatientProfile> profiles = patientProfileRepository.findByUserIdIn(appointmentsByUser.keySet());
        var stream = profiles.stream()
                .filter(p -> p.getUser() != null)
                .filter(p -> name == null || p.getUser().getName().toLowerCase().contains(name.toLowerCase()))
                .filter(p -> cpf == null || (p.getCpf() != null && p.getCpf().equalsIgnoreCase(cpf)))
                .filter(p -> {
                    if (date == null) return true;
                    var d = LocalDate.parse(date);
                    var start = d.atStartOfDay().toInstant(ZoneOffset.UTC);
                    var end = d.atTime(java.time.LocalTime.MAX).toInstant(ZoneOffset.UTC);
                    List<com.passmais.domain.entity.Appointment> patientAppointments = appointmentsByUser.getOrDefault(p.getUser().getId(), List.of());
                    return patientAppointments.stream().anyMatch(a -> {
                        Instant dt = a.getDateTime();
                        return !dt.isBefore(start) && !dt.isAfter(end);
                    });
                });
        return ResponseEntity.ok(stream.toList());
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/{id}/photo")
    public ResponseEntity<DoctorPublicProfileDTO> uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) throws java.io.IOException {
        DoctorProfile d = doctorRepo.findById(id).orElseThrow();
        storeDoctorPhoto(d, file);
        DoctorProfile saved = doctorRepo.save(d);
        return ResponseEntity.ok(toPublic(saved));
    }

    private DoctorProfile updateDoctorProfile(UUID id, DoctorPublicProfileDTO dto, MultipartFile photo) throws java.io.IOException {
        DoctorProfile doctor = doctorRepo.findById(id).orElseThrow();
        applyDoctorProfileData(doctor, dto);
        if (photo != null && !photo.isEmpty()) {
            storeDoctorPhoto(doctor, photo);
        }
        return doctorRepo.save(doctor);
    }

    private void applyDoctorProfileData(DoctorProfile doctor, DoctorPublicProfileDTO dto) {
        doctor.setBio(dto.bio());
        doctor.setSpecialty(dto.specialty());
        doctor.setCrm(dto.crm());
        doctor.setConsultationPrice(dto.consultationPrice());
        doctor.setClinicName(dto.clinicName());
        doctor.setClinicStreetAndNumber(dto.clinicStreetAndNumber());
        doctor.setClinicCity(dto.clinicCity());
        doctor.setClinicPostalCode(dto.clinicPostalCode());
    }

    private void storeDoctorPhoto(DoctorProfile doctor, MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            return;
        }
        java.nio.file.Path dir = java.nio.file.Paths.get("uploads");
        java.nio.file.Files.createDirectories(dir);
        String filename = doctor.getId() + "-" + java.util.UUID.randomUUID() + "-" + file.getOriginalFilename();
        java.nio.file.Path path = dir.resolve(filename);
        file.transferTo(path.toFile());
        doctor.setPhotoUrl("/uploads/" + filename);
    }
}
