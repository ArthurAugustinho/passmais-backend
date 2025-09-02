package com.passmais.interfaces.controller;

import com.passmais.application.service.AuthService;
import com.passmais.domain.entity.User;
import com.passmais.interfaces.dto.*;
import com.passmais.interfaces.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenDTO> login(@RequestBody @Valid AuthRequestDTO request) {
        Map<String, String> tokens = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthTokenDTO(tokens.get("accessToken"), tokens.get("refreshToken")));
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
        User user = userMapper.toEntity(dto);
        User saved = authService.register(user, dto.password());
        return ResponseEntity.ok(userMapper.toResponse(saved));
    }
}
