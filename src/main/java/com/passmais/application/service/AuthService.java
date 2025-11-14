package com.passmais.application.service;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorSecretary;
import com.passmais.domain.entity.TeamInvite;
import com.passmais.domain.entity.User;
import com.passmais.domain.util.EmailUtils;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.DoctorSecretaryRepository;
import com.passmais.infrastructure.repository.TeamInviteRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorSecretaryRepository doctorSecretaryRepository;
    private final TeamInviteRepository teamInviteRepository;

    // Bloqueio por tentativas desativado

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       DoctorProfileRepository doctorProfileRepository,
                       DoctorSecretaryRepository doctorSecretaryRepository,
                       TeamInviteRepository teamInviteRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorSecretaryRepository = doctorSecretaryRepository;
        this.teamInviteRepository = teamInviteRepository;
    }

    public Map<String, String> login(String email, String rawPassword) {
        String normalizedEmail = EmailUtils.normalize(email);
        if (normalizedEmail == null) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        // Sem verificação de bloqueio por tentativas

        // Verificação de e-mail: pacientes e médicos podem logar sem verificar e-mail
        if (user.getEmailVerifiedAt() == null) {
            if (!(user.getRole() == Role.PATIENT || user.getRole() == Role.DOCTOR || user.getRole() == Role.SECRETARY)) {
                throw new BadCredentialsException("E-mail não verificado");
            }
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, rawPassword));
        } catch (BadCredentialsException ex) {
            // Apenas registra tentativa falha sem bloquear a conta
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        // sucesso: reset tentativas
        if (!normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
        }
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null); // redundante, mas mantém compatibilidade
        user.setLastTokenRevalidatedAt(Instant.now());
        userRepository.save(user);

        Map<String, Object> claims = buildClaims(user);
        String accessToken = jwtService.generateAccessToken(user.getEmail(), claims);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    public Map<String, String> refresh(String refreshToken) {
        if (!jwtService.isValid(refreshToken)) {
            throw new BadCredentialsException("Token de refresh inválido");
        }
        String email = jwtService.getSubject(refreshToken);
        String normalizedEmail = EmailUtils.normalize(email);
        if (normalizedEmail == null) {
            throw new BadCredentialsException("Usuário inválido");
        }
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Usuário inválido"));

        // Revalidação a cada 24h: um novo refresh token é emitido e a data é atualizada
        if (!normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
        }
        user.setLastTokenRevalidatedAt(Instant.now());
        userRepository.save(user);

        Map<String, Object> claims = buildClaims(user);
        String newAccessToken = jwtService.generateAccessToken(user.getEmail(), claims);
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);
        return tokens;
    }

    public User register(User user, String rawPassword) {
        String normalizedEmail = EmailUtils.normalize(user.getEmail());
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId().toString());
        if (user.getRole() == Role.DOCTOR) {
            doctorProfileRepository.findByUserId(user.getId())
                    .map(profile -> profile.getId().toString())
                    .ifPresent(id -> claims.put("doctorId", id));
        } else if (user.getRole() == Role.SECRETARY) {
            List<DoctorSecretary> links =
                    doctorSecretaryRepository.findAllByIdSecretaryIdAndActiveTrue(user.getId());

            List<Map<String, String>> doctorSummaries = new ArrayList<>();
            Map<UUID, DoctorProfile> profilesByUserId = new HashMap<>();
            Set<UUID> seenDoctors = new HashSet<>();

            if (!links.isEmpty()) {
                List<UUID> doctorUserIds = links.stream()
                        .map(link -> link.getDoctor().getId())
                        .distinct()
                        .toList();

                profilesByUserId.putAll(
                        doctorProfileRepository.findByUserIdIn(doctorUserIds).stream()
                                .collect(Collectors.toMap(profile -> profile.getUser().getId(), profile -> profile))
                );

                for (DoctorSecretary link : links) {
                    appendDoctorSummary(doctorSummaries, profilesByUserId, link.getDoctor(), seenDoctors);
                }
            }

            String normalizedEmail = EmailUtils.normalize(user.getEmail());
            if (normalizedEmail != null) {
                List<TeamInvite> invites = teamInviteRepository.findAllBySecretaryCorporateEmailIgnoreCase(normalizedEmail);
                if (!invites.isEmpty()) {
                    for (TeamInvite invite : invites) {
                        User doctor = invite.getDoctor();
                        if (doctor == null || seenDoctors.contains(doctor.getId())) {
                            continue;
                        }
                        if (!profilesByUserId.containsKey(doctor.getId())) {
                            doctorProfileRepository.findByUserId(doctor.getId())
                                    .ifPresent(profile -> profilesByUserId.put(doctor.getId(), profile));
                        }
                        appendDoctorSummary(doctorSummaries, profilesByUserId, doctor, seenDoctors);
                    }
                }
            }

            claims.put("doctors", doctorSummaries);
        }
        return claims;
    }

    private void appendDoctorSummary(List<Map<String, String>> target,
                                     Map<UUID, DoctorProfile> profilesByUserId,
                                     User doctorUser,
                                     Set<UUID> seenDoctors) {
        if (doctorUser == null) {
            return;
        }
        if (!seenDoctors.add(doctorUser.getId())) {
            return;
        }
        DoctorProfile profile = profilesByUserId.get(doctorUser.getId());
        Map<String, String> entry = new HashMap<>();
        String doctorId = profile != null
                ? profile.getId().toString()
                : doctorUser.getId().toString();
        entry.put("doctorId", doctorId);
        entry.put("fullName", doctorUser.getName());
        target.add(entry);
    }
}
