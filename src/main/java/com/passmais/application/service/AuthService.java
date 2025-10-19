package com.passmais.application.service;

import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DoctorProfileRepository doctorProfileRepository;

    // Bloqueio por tentativas desativado

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       DoctorProfileRepository doctorProfileRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.doctorProfileRepository = doctorProfileRepository;
    }

    public Map<String, String> login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        // Sem verificação de bloqueio por tentativas

        // Verificação de e-mail: pacientes e médicos podem logar sem verificar e-mail
        if (user.getEmailVerifiedAt() == null) {
            if (!(user.getRole() == Role.PATIENT || user.getRole() == Role.DOCTOR)) {
                throw new BadCredentialsException("E-mail não verificado");
            }
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (BadCredentialsException ex) {
            // Apenas registra tentativa falha sem bloquear a conta
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        // sucesso: reset tentativas
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
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Usuário inválido"));

        // Revalidação a cada 24h: um novo refresh token é emitido e a data é atualizada
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
        }
        return claims;
    }
}
