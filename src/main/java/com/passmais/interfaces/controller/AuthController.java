package com.passmais.interfaces.controller;

import com.passmais.application.service.AuthService;
import com.passmais.domain.entity.User;
import com.passmais.domain.entity.RevokedToken;
import com.passmais.interfaces.dto.*;
import com.passmais.interfaces.mapper.UserMapper;
import com.passmais.infrastructure.repository.RevokedTokenRepository;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.infrastructure.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserMapper userMapper, RevokedTokenRepository revokedTokenRepository, JwtService jwtService, UserRepository userRepository) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthRequestDTO request) {
        Map<String, String> tokens = authService.login(request.email(), request.password());
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        return ResponseEntity.ok(new LoginResponseDTO(tokens.get("accessToken"), user.getName(), user.getRole()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenDTO> refresh(@RequestBody @Valid RefreshTokenDTO request) {
        Map<String, String> tokens = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new AuthTokenDTO(tokens.get("accessToken"), tokens.get("refreshToken")));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid UserCreateDTO dto) {
        if (dto.role() == com.passmais.domain.enums.Role.PATIENT) {
            if (dto.phone() == null || dto.phone().isBlank()) {
                throw new IllegalArgumentException("Telefone é obrigatório para cadastro de paciente");
            }
        }
        if (dto.role() == com.passmais.domain.enums.Role.ADMIN ||
            dto.role() == com.passmais.domain.enums.Role.ADMINISTRATOR ||
            dto.role() == com.passmais.domain.enums.Role.SUPERADMIN) {
            throw new IllegalArgumentException("Criação de administradores deve ser feita por SUPERADMIN em endpoint dedicado");
        }
        User user = userMapper.toEntity(dto);
        User saved = authService.register(user, dto.password());
        return ResponseEntity.ok(userMapper.toResponse(saved));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = org.springframework.http.HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtService.isValid(token)) {
                java.time.Instant exp = jwtService.getExpiration(token);
                RevokedToken rt = RevokedToken.builder()
                        .token(token)
                        .expiresAt(exp)
                        .build();
                revokedTokenRepository.save(rt);
            }
        }
        return ResponseEntity.noContent().build();
    }
}
