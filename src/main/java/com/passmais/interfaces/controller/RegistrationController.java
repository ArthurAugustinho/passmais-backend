package com.passmais.interfaces.controller;

import com.passmais.application.service.AuthService;
import com.passmais.application.service.S3StorageService;
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
    private final S3StorageService s3StorageService;

    public RegistrationController(AuthService authService, UserRepository userRepository, DoctorProfileRepository doctorProfileRepository, PatientProfileRepository patientProfileRepository, EmailService emailService, S3StorageService s3StorageService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.emailService = emailService;
        this.s3StorageService = s3StorageService;
    }

    @PostMapping("/doctor")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerDoctor(@RequestBody @Valid DoctorRegisterDTO dto) {
        Map<String, Object> response = registerDoctorInternal(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/doctor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> registerDoctorMultipart(
            @RequestPart("doctor") @Valid DoctorRegisterDTO dto,
            @RequestPart("photoUrl") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo de imagem enviado.");
        }

        Map<String, Object> response = registerDoctorInternal(dto);
        UUID userId = (UUID) response.get("userId");
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId).orElseThrow();

        try {
            String photoUrl = s3StorageService.uploadDoctorPhoto(image, profile.getId());
            profile.setPhotoUrl(photoUrl);
            doctorProfileRepository.save(profile);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Erro ao carregar a imagem para o S3", ex);
        }

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> registerDoctorInternal(DoctorRegisterDTO dto) {
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
        return Map.of(
                "userId", user.getId(),
                "verificationCode", code
        );
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

    // Removed legacy form endpoint in favor of multipart handler above
}
