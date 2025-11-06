package com.passmais.interfaces.controller;

import com.passmais.application.service.TeamService;
import com.passmais.domain.entity.User;
import com.passmais.domain.enums.Role;
import com.passmais.domain.util.EmailUtils;
import com.passmais.interfaces.dto.team.*;
import com.passmais.infrastructure.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PostMapping("/invite")
    public ResponseEntity<TeamInviteResponseDTO> createInvite(@Valid @RequestBody TeamInviteRequestDTO request) {
        User doctor = requireAuthenticatedUser(Role.DOCTOR);
        TeamService.InviteCreationResult result = teamService.createInvite(
                doctor.getId(),
                request.maxUses(),
                request.expiresAt(),
                request.secretaryFullName(),
                request.secretaryCorporateEmail()
        );
        var invite = result.invite();
        TeamInviteResponseDTO response = new TeamInviteResponseDTO(
                result.code(),
                invite.getStatus().name(),
                invite.getUsesRemaining(),
                invite.getExpiresAt()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<TeamJoinResponseDTO> joinTeam(@Valid @RequestBody TeamJoinRequestDTO request, HttpServletRequest httpRequest) {
        UUID authenticatedSecretaryId = resolveAuthenticatedUser()
                .filter(user -> user.getRole() == Role.SECRETARY)
                .map(User::getId)
                .orElse(null);
        TeamService.JoinTeamResult result = teamService.joinTeam(
                new TeamService.JoinTeamCommand(
                        request.inviteCode(),
                        request.email(),
                        request.password(),
                        request.name(),
                        authenticatedSecretaryId,
                        resolveClientIp(httpRequest),
                        httpRequest != null ? httpRequest.getHeader("User-Agent") : null
                )
        );
        TeamJoinResponseDTO response = new TeamJoinResponseDTO(
                "Secretária vinculada ao médico com sucesso.",
                result.secretary().getRole(),
                new TeamJoinResponseDTO.LinkedDoctorDTO(
                        result.doctor().getId(),
                        result.doctor().getName()
                )
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/secretaries")
    public ResponseEntity<List<TeamSecretaryResponseDTO>> listSecretaries() {
        User doctor = requireAuthenticatedUser(Role.DOCTOR);
        List<TeamSecretaryResponseDTO> response = teamService.listSecretaries(doctor.getId()).stream()
                .map(item -> new TeamSecretaryResponseDTO(
                        item.secretary().getId(),
                        item.secretary().getName(),
                        item.secretary().getEmail(),
                        item.linkedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('SECRETARY')")
    @GetMapping("/doctors")
    public ResponseEntity<List<TeamDoctorResponseDTO>> listDoctors() {
        User secretary = requireAuthenticatedUser(Role.SECRETARY);
        List<TeamDoctorResponseDTO> response = teamService.listDoctors(secretary.getId()).stream()
                .map(item -> new TeamDoctorResponseDTO(
                        item.doctor().getId(),
                        item.doctor().getName(),
                        item.profile() != null ? item.profile().getSpecialty() : null
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @PatchMapping("/invite/{code}/revoke")
    public ResponseEntity<Map<String, String>> revokeInvite(@PathVariable String code) {
        User doctor = requireAuthenticatedUser(Role.DOCTOR);
        teamService.revokeInvite(doctor.getId(), code);
        return ResponseEntity.ok(Map.of("message", "Convite revogado com sucesso."));
    }

    @PreAuthorize("hasAnyRole('DOCTOR','SECRETARY')")
    @DeleteMapping("/link")
    public ResponseEntity<Map<String, String>> revokeLink(@Valid @RequestBody TeamLinkDeleteRequestDTO request) {
        User actor = resolveAuthenticatedUser()
                .orElseThrow(() -> new AccessDeniedException("Usuário não autenticado"));
        teamService.revokeLink(actor.getId(), actor.getRole(), request.doctorId(), request.secretaryId());
        return ResponseEntity.ok(Map.of("message", "Vínculo removido com sucesso."));
    }

    private User requireAuthenticatedUser(Role requiredRole) {
        User user = resolveAuthenticatedUser()
                .orElseThrow(() -> new AccessDeniedException("Usuário não autenticado"));
        if (user.getRole() != requiredRole) {
            throw new AccessDeniedException("Acesso negado para o perfil atual");
        }
        return user;
    }

    private Optional<User> resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        String username = extractUsername(authentication);
        if (username == null) {
            return Optional.empty();
        }
        String normalized = EmailUtils.normalize(username);
        return userRepository.findByEmailIgnoreCase(normalized != null ? normalized : username);
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s) {
            return s;
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
