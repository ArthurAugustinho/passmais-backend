package com.passmais.interfaces.controller;

import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.infrastructure.repository.UserRepository;
import com.passmais.infrastructure.repository.AuditLogRepository;
import com.passmais.domain.entity.AuditLog;
import com.passmais.interfaces.dto.AdminUserCreateDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PostMapping
    public ResponseEntity<User> create(@RequestBody AdminUserCreateDTO dto) {
        if (dto.role() != Role.ADMINISTRATOR) {
            throw new IllegalArgumentException("Somente criação de ADMINISTRATOR neste endpoint");
        }
        User u = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .role(dto.role())
                .build();
        User saved = userRepository.save(u);
        auditLogRepository.save(AuditLog.builder().actorUserId(saved.getId()).action("ADMIN_CREATE").details("Admin created: " + saved.getEmail()).build());
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userRepository.deleteById(id);
        auditLogRepository.save(AuditLog.builder().actorUserId(id).action("ADMIN_DELETE").details("Admin deleted: " + id).build());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable UUID id, @RequestBody AdminUserCreateDTO dto) {
        User u = userRepository.findById(id).orElseThrow();
        if (dto.role() != Role.ADMINISTRATOR) {
            throw new IllegalArgumentException("Somente ADMINISTRATOR");
        }
        u.setName(dto.name());
        u.setEmail(dto.email());
        u.setRole(dto.role());
        if (dto.password() != null && !dto.password().isBlank()) {
            u.setPassword(passwordEncoder.encode(dto.password()));
        }
        User saved = userRepository.save(u);
        auditLogRepository.save(AuditLog.builder().actorUserId(id).action("ADMIN_UPDATE").details("Admin updated: " + saved.getEmail()).build());
        return ResponseEntity.ok(saved);
    }
}
