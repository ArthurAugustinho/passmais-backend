package com.passmais.interfaces.controller;

import com.passmais.application.service.AuthService;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.infrastructure.validation.CPFValidator;
import com.passmais.infrastructure.service.EmailService;
import com.passmais.interfaces.dto.DoctorRegisterDTO;
import com.passmais.interfaces.dto.PatientRegisterDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final EmailService emailService;

    public RegistrationController(AuthService authService, UserRepository userRepository, DoctorProfileRepository doctorProfileRepository, PatientProfileRepository patientProfileRepository, EmailService emailService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.emailService = emailService;
    }

    @PostMapping("/doctor")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerDoctor(@RequestBody @Valid DoctorRegisterDTO dto) {
        if (!Boolean.TRUE.equals(dto.lgpdAccepted())) {
            throw new IllegalArgumentException("Aceite da LGPD é obrigatório");
        }
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new IllegalArgumentException("Senha e confirmação não conferem");
        }
        if (!CPFValidator.isValid(dto.cpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (!com.passmais.infrastructure.validation.CRMValidator.isValid(dto.crm())) {
            throw new IllegalArgumentException("CRM inválido");
        }
        if (!dto.phone().matches("^\\+?[0-9]{10,14}$")) {
            throw new IllegalArgumentException("Telefone inválido");
        }
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        if (doctorProfileRepository.existsByPhone(dto.phone()) || patientProfileRepository.existsByCellPhone(dto.phone())) {
            throw new IllegalArgumentException("Telefone já utilizado");
        }
        if (doctorProfileRepository.existsByCpf(dto.cpf())) {
            throw new IllegalArgumentException("CPF já utilizado");
        }
        if (doctorProfileRepository.existsByCrm(dto.crm())) {
            throw new IllegalArgumentException("CRM já utilizado");
        }
        String code = java.util.UUID.randomUUID().toString().replaceAll("-", "");
        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .role(Role.DOCTOR)
                .lgpdAcceptedAt(Instant.now())
                .verificationCode(code)
                .build();
        user = authService.register(user, dto.password());

        DoctorProfile profile = DoctorProfile.builder()
                .user(user)
                .crm(dto.crm())
                .specialty(dto.specialty())
                .bio(dto.bio())
                .phone(dto.phone())
                .cpf(dto.cpf())
                .birthDate(dto.birthDate())
                .photoUrl(dto.photoUrl())
                .consultationPrice(dto.consultationPrice())
                .approved(false)
                .build();
        doctorProfileRepository.save(profile);

        emailService.sendVerificationEmail(user.getEmail(), code);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "verificationCode", code
        ));
    }

    @PostMapping("/patient")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerPatient(@RequestBody @Valid PatientRegisterDTO dto) {
        if (!Boolean.TRUE.equals(dto.lgpdAccepted())) {
            throw new IllegalArgumentException("Aceite da LGPD é obrigatório");
        }
        if (!CPFValidator.isValid(dto.cpf())) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (patientProfileRepository.existsByCellPhone(dto.cellPhone())) {
            throw new IllegalArgumentException("Telefone já utilizado");
        }
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        String code = java.util.UUID.randomUUID().toString().replaceAll("-", "");
        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .role(Role.PATIENT)
                .lgpdAcceptedAt(Instant.now())
                .verificationCode(code)
                .build();
        user = authService.register(user, dto.password());

        PatientProfile profile = PatientProfile.builder()
                .user(user)
                .cpf(dto.cpf())
                .birthDate(dto.birthDate())
                .address(dto.address())
                .cellPhone(dto.cellPhone())
                .communicationPreference(dto.communicationPreference())
                .build();
        patientProfileRepository.save(profile);

        emailService.sendVerificationEmail(user.getEmail(), code);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "verificationCode", code
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("email") String email, @RequestParam("code") String code) {
        User user = userRepository.findByEmail(email).orElseThrow();
        if (user.getEmailVerifiedAt() != null) {
            return ResponseEntity.noContent().build();
        }
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new IllegalArgumentException("Código de verificação inválido");
        }
        user.setEmailVerifiedAt(Instant.now());
        user.setVerificationCode(null);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    // Multipart alternative that accepts photo upload
    @PostMapping(value = "/doctor/form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> registerDoctorForm(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam Boolean lgpdAccepted,
            @RequestParam String crm,
            @RequestParam String phone,
            @RequestParam String cpf,
            @RequestParam String birthDate,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) java.math.BigDecimal consultationPrice,
            @RequestPart(required = false) MultipartFile photo
    ) throws java.io.IOException {
        DoctorRegisterDTO dto = new DoctorRegisterDTO(
                name, email, password, confirmPassword, lgpdAccepted, crm, phone, cpf,
                java.time.LocalDate.parse(birthDate), bio, specialty, consultationPrice, null
        );
        var resp = registerDoctor(dto).getBody();
        // Save photo if present
        if (photo != null && !photo.isEmpty()) {
            UUID userId = (UUID) resp.get("userId");
            User user = userRepository.findById(userId).orElseThrow();
            DoctorProfile profile = doctorProfileRepository.findAll().stream().filter(p -> p.getUser().getId().equals(user.getId())).findFirst().orElseThrow();
            java.nio.file.Path dir = java.nio.file.Paths.get("uploads");
            java.nio.file.Files.createDirectories(dir);
            String filename = profile.getId() + "-" + java.util.UUID.randomUUID() + "-" + photo.getOriginalFilename();
            java.nio.file.Path path = dir.resolve(filename);
            photo.transferTo(path.toFile());
            profile.setPhotoUrl("/uploads/" + filename);
            doctorProfileRepository.save(profile);
        }
        return ResponseEntity.ok(resp);
    }
}
