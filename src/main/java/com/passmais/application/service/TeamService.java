package com.passmais.application.service;

import com.passmais.application.service.exception.InviteSecurityException;
import com.passmais.domain.entity.*;
import com.passmais.domain.enums.InviteAuditStatus;
import com.passmais.domain.enums.Role;
import com.passmais.domain.enums.TeamInviteStatus;
import com.passmais.domain.util.EmailUtils;
import com.passmais.infrastructure.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class TeamService {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 8;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final SecureRandom secureRandom = new SecureRandom();
    private final TeamInviteRepository teamInviteRepository;
    private final DoctorSecretaryRepository doctorSecretaryRepository;
    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final InviteAuditLogRepository inviteAuditLogRepository;
    private final AuthService authService;

    public TeamService(TeamInviteRepository teamInviteRepository,
                       DoctorSecretaryRepository doctorSecretaryRepository,
                       UserRepository userRepository,
                       DoctorProfileRepository doctorProfileRepository,
                       InviteAuditLogRepository inviteAuditLogRepository,
                       AuthService authService) {
        this.teamInviteRepository = teamInviteRepository;
        this.doctorSecretaryRepository = doctorSecretaryRepository;
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.inviteAuditLogRepository = inviteAuditLogRepository;
        this.authService = authService;
    }

    @Transactional
    public InviteCreationResult createInvite(UUID doctorUserId,
                                             int requestedMaxUses,
                                             Instant expiresAt,
                                             String secretaryFullName,
                                             String secretaryCorporateEmail) {
        final int allowedMaxUses = 1;
        if (requestedMaxUses != allowedMaxUses) {
            throw new IllegalArgumentException("Convites permitem apenas um uso");
        }
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("expiresAt deve ser uma data futura");
        }
        String normalizedSecretaryName = normalizeFullName(secretaryFullName);
        if (normalizedSecretaryName == null) {
            throw new IllegalArgumentException("Nome completo da secretária é obrigatório");
        }
        String normalizedCorporateEmail = EmailUtils.normalize(secretaryCorporateEmail);
        if (normalizedCorporateEmail == null) {
            throw new IllegalArgumentException("E-mail corporativo da secretária é obrigatório");
        }

        User doctor = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new IllegalArgumentException("Médico não encontrado"));
        if (doctor.getRole() != Role.DOCTOR) {
            throw new IllegalArgumentException("Usuário informado não é um médico");
        }

        String rawCode;
        String hashedCode;
        int attempts = 0;
        do {
            if (attempts++ > 25) {
                throw new IllegalStateException("Não foi possível gerar código único para convite");
            }
            rawCode = generateCodeCandidate();
            hashedCode = hashCode(rawCode);
        } while (teamInviteRepository.existsByInviteCodeHash(hashedCode));

        TeamInvite invite = TeamInvite.builder()
                .doctor(doctor)
                .inviteCodeHash(hashedCode)
                .maxUses(allowedMaxUses)
                .usesRemaining(allowedMaxUses)
                .displayCode(rawCode)
                .secretaryFullName(normalizedSecretaryName)
                .secretaryCorporateEmail(normalizedCorporateEmail)
                .expiresAt(expiresAt)
                .status(TeamInviteStatus.ACTIVE)
                .failedAttempts(0)
                .build();
        TeamInvite saved = teamInviteRepository.save(invite);
        return new InviteCreationResult(rawCode, saved);
    }

    @Transactional
    public JoinTeamResult joinTeam(JoinTeamCommand command) {
        if (command.inviteCode() == null || command.inviteCode().isBlank()) {
            throw new IllegalArgumentException("Código de convite é obrigatório");
        }
        if (command.email() == null || command.email().isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório");
        }

        Instant now = Instant.now();
        String normalizedCode = command.inviteCode().trim().toUpperCase(Locale.ROOT);
        String inviteHash = hashCode(normalizedCode);
        TeamInvite invite = teamInviteRepository.findByInviteCodeHash(inviteHash)
                .orElseThrow(() -> {
                    throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
                });

        if (invite.getStatus() == TeamInviteStatus.REVOKED) {
            audit(invite, null, InviteAuditStatus.REVOKED, "Código revogado", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
        }
        if (invite.getStatus() == TeamInviteStatus.BLOCKED) {
            audit(invite, null, InviteAuditStatus.BLOCKED, "Código bloqueado", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
        }
        if (invite.isExpired(now)) {
            invite.markExpired();
            teamInviteRepository.save(invite);
            audit(invite, null, InviteAuditStatus.EXPIRED, "Código expirado", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
        }
        if (!invite.hasRemainingUses()) {
            invite.decrementUse(); // garante atualização de status para EXHAUSTED
            teamInviteRepository.save(invite);
            audit(invite, null, InviteAuditStatus.FAILED, "Limite de usos atingido", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
        }
        if (invite.getUsesRemaining() != 1) {
            audit(invite, null, InviteAuditStatus.FAILED, "Código não está habilitado para uso único", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
        }

        User doctor = invite.getDoctor();
        if (doctor.getRole() != Role.DOCTOR) {
            audit(invite, null, InviteAuditStatus.FAILED, "Convite sem médico válido", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
        }

        String normalizedEmail = EmailUtils.normalize(command.email());
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("E-mail é obrigatório");
        }
        String normalizedName = normalizeFullName(command.name());
        if (normalizedName == null) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (!invite.getSecretaryFullName().equalsIgnoreCase(normalizedName)) {
            invite.incrementFailedAttempts();
            teamInviteRepository.save(invite);
            audit(invite, null, InviteAuditStatus.FAILED, "Nome fornecido divergente do cadastro do convite", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Nome completo ou e-mail corporativo não coincidem com os dados do código de convite.");
        }
        if (!invite.getSecretaryCorporateEmail().equals(normalizedEmail)) {
            invite.incrementFailedAttempts();
            teamInviteRepository.save(invite);
            audit(invite, null, InviteAuditStatus.FAILED, "E-mail fornecido divergente do cadastro do convite", command);
            throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Nome completo ou e-mail corporativo não coincidem com os dados do código de convite.");
        }

        User secretary = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (secretary == null && command.expectedSecretaryId() != null) {
            invite.incrementFailedAttempts();
            teamInviteRepository.save(invite);
            handleFailedAttempt(invite, InviteAuditStatus.FAILED, "Usuário autenticado não corresponde ao e-mail informado", command);
        }

        boolean createdUser = false;
        if (secretary == null) {
            validateNewSecretaryData(command);
            secretary = buildAndRegisterSecretary(normalizedEmail, normalizedName, command);
            createdUser = true;
        } else {
            if (secretary.getRole() != Role.SECRETARY) {
                invite.incrementFailedAttempts();
                teamInviteRepository.save(invite);
                handleFailedAttempt(invite, InviteAuditStatus.FAILED, "E-mail associado a outro perfil", command);
            }
            if (command.expectedSecretaryId() != null && !secretary.getId().equals(command.expectedSecretaryId())) {
                invite.incrementFailedAttempts();
                teamInviteRepository.save(invite);
                handleFailedAttempt(invite, InviteAuditStatus.FAILED, "Usuário autenticado não corresponde ao e-mail informado", command);
            }
            boolean altered = updateExistingSecretary(secretary, command, normalizedName);
            if (altered) {
                userRepository.save(secretary);
            }
        }

        Optional<DoctorSecretary> existingLink = doctorSecretaryRepository.findByIdDoctorIdAndIdSecretaryId(doctor.getId(), secretary.getId());
        DoctorSecretary link;
        boolean createdLink = false;
        boolean reactivatedLink = false;
        if (existingLink.isPresent()) {
            link = existingLink.get();
            if (!link.isActive()) {
                link.setActive(true);
                link.setLinkedAt(now);
                reactivatedLink = true;
            }
        } else {
            link = DoctorSecretary.builder()
                    .id(DoctorSecretaryId.builder()
                            .doctorId(doctor.getId())
                            .secretaryId(secretary.getId())
                            .build())
                    .doctor(doctor)
                    .secretary(secretary)
                    .linkedAt(now)
                    .active(true)
                    .build();
            createdLink = true;
        }
        doctorSecretaryRepository.save(link);

        invite.decrementUse();
        invite.markRevoked();
        invite.resetFailedAttempts();
        teamInviteRepository.save(invite);

        audit(invite, secretary, InviteAuditStatus.SUCCESS, "Vínculo criado/reativado", command);

        Optional<DoctorProfile> doctorProfile = doctorProfileRepository.findByUserId(doctor.getId());

        return new JoinTeamResult(doctor, doctorProfile.orElse(null), secretary, link, createdUser, createdLink || reactivatedLink);
    }

    @Transactional(readOnly = true)
    public List<SecretaryListing> listSecretaries(UUID doctorUserId) {
        List<DoctorSecretary> links = doctorSecretaryRepository.findAllByIdDoctorIdAndActiveTrue(doctorUserId);
        return links.stream()
                .map(link -> new SecretaryListing(link.getSecretary(), link.getLinkedAt()))
                .toList();
    }

    @Transactional
    public List<InviteSummary> listActiveInvites(UUID doctorUserId) {
        Instant now = Instant.now();
        List<TeamInvite> invites = teamInviteRepository.findAllByDoctorId(doctorUserId);
        List<TeamInvite> expiredToPersist = new ArrayList<>();
        List<InviteSummary> availableInvites = new ArrayList<>();

        for (TeamInvite invite : invites) {
            if (invite.isExpired(now)) {
                if (invite.getStatus() != TeamInviteStatus.EXPIRED) {
                    invite.markExpired();
                    expiredToPersist.add(invite);
                }
                continue;
            }
            if (!invite.isActive() || !invite.hasRemainingUses()) {
                continue;
            }
            String displayCode = invite.getDisplayCode();
            if (displayCode == null || displayCode.isBlank()) {
                continue;
            }
            availableInvites.add(new InviteSummary(
                    displayCode,
                    invite.getStatus(),
                    invite.getUsesRemaining(),
                    invite.getExpiresAt(),
                    invite.getSecretaryFullName(),
                    invite.getSecretaryCorporateEmail()
            ));
        }

        if (!expiredToPersist.isEmpty()) {
            teamInviteRepository.saveAll(expiredToPersist);
        }
        return availableInvites;
    }

    @Transactional(readOnly = true)
    public List<DoctorListing> listDoctors(UUID secretaryUserId) {
        List<DoctorSecretary> links = doctorSecretaryRepository.findAllByIdSecretaryIdAndActiveTrue(secretaryUserId);
        if (links.isEmpty()) {
            return List.of();
        }
        List<UUID> doctorUserIds = links.stream()
                .map(link -> link.getDoctor().getId())
                .distinct()
                .toList();
        var profiles = doctorProfileRepository.findByUserIdIn(doctorUserIds).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getUser().getId(), p -> p));
        return links.stream()
                .map(link -> new DoctorListing(link.getDoctor(), profiles.get(link.getDoctor().getId())))
                .toList();
    }

    @Transactional
    public void revokeLink(UUID actorId, Role actorRole, UUID doctorProfileId, UUID secretaryId) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil de médico não encontrado"));
        UUID doctorUserId = doctorProfile.getUser().getId();

        if (actorRole == Role.DOCTOR && !actorId.equals(doctorUserId)) {
            throw new IllegalArgumentException("Médico não autorizado a revogar este vínculo");
        }
        if (actorRole == Role.SECRETARY && !actorId.equals(secretaryId)) {
            throw new IllegalArgumentException("Secretária não autorizada a revogar este vínculo");
        }
        if (actorRole != Role.DOCTOR && actorRole != Role.SECRETARY) {
            throw new IllegalArgumentException("Perfil não autorizado para revogação");
        }

        DoctorSecretary link = doctorSecretaryRepository.findByIdDoctorIdAndIdSecretaryId(doctorUserId, secretaryId)
                .orElseThrow(() -> new IllegalArgumentException("Vínculo não encontrado"));

        if (!link.isActive()) {
            return;
        }

        link.setActive(false);
        doctorSecretaryRepository.save(link);
    }

    @Transactional
    public void revokeInvite(UUID doctorId, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalArgumentException("Código é obrigatório");
        }
        String hash = hashCode(inviteCode.trim().toUpperCase(Locale.ROOT));
        TeamInvite invite = teamInviteRepository.findByDoctorIdAndInviteCodeHash(doctorId, hash)
                .orElseThrow(() -> new IllegalArgumentException("Convite não encontrado"));
        invite.markRevoked();
        teamInviteRepository.save(invite);
        audit(invite, null, InviteAuditStatus.REVOKED, "Revogação manual solicitada pelo médico", null);
    }

    private void validateNewSecretaryData(JoinTeamCommand command) {
        if (command.password() == null || command.password().isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória para novo cadastro");
        }
        if (command.password().length() < 8) {
            throw new IllegalArgumentException("Senha deve ter ao menos 8 caracteres");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório para novo cadastro");
        }
        // Dados pessoais adicionais (telefone/CPF) não são mais capturados neste fluxo;
        // mantém apenas validações essenciais.
    }

    private User buildAndRegisterSecretary(String email, String normalizedName, JoinTeamCommand command) {
        String effectiveName = normalizedName != null ? normalizedName : normalizeFullName(command.name());
        if (effectiveName == null) {
            effectiveName = command.name();
        }
        User user = User.builder()
                .name(effectiveName)
                .email(email)
                .role(Role.SECRETARY)
                .lgpdAcceptedAt(Instant.now())
                .emailVerifiedAt(Instant.now())
                .build();
        return authService.register(user, command.password());
    }

    private boolean updateExistingSecretary(User secretary, JoinTeamCommand command, String normalizedName) {
        boolean altered = false;
        String effectiveName = normalizedName != null ? normalizedName : normalizeFullName(command.name());
        if (effectiveName != null && !effectiveName.equals(secretary.getName())) {
            secretary.setName(effectiveName);
            altered = true;
        }
        return altered;
    }

    private void handleFailedAttempt(TeamInvite invite, InviteAuditStatus status, String details, JoinTeamCommand command) {
        audit(invite, null, status, details, command);
        throw new InviteSecurityException(HttpStatus.BAD_REQUEST, "Código inválido, expirado ou não autorizado.");
    }

    private void audit(TeamInvite invite, User secretary, InviteAuditStatus status, String details, JoinTeamCommand command) {
        InviteAuditLog audit = InviteAuditLog.builder()
                .invite(invite)
                .secretary(secretary)
                .ipAddress(command != null ? command.ipAddress() : null)
                .userAgent(command != null ? command.userAgent() : null)
                .status(status)
                .details(details)
                .build();
        inviteAuditLogRepository.save(audit);
    }

    private String generateCodeCandidate() {
        char[] buffer = new char[CODE_LENGTH + 1];
        int idx = 0;
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i == CODE_LENGTH / 2) {
                buffer[idx++] = '-';
            }
            buffer[idx++] = CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)];
        }
        return new String(buffer, 0, idx);
    }

    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }

    private String normalizeFullName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("\\s+", " ");
    }

    public record InviteCreationResult(String code, TeamInvite invite) {}

    public record JoinTeamCommand(String inviteCode,
                                  String email,
                                  String password,
                                  String name,
                                  UUID expectedSecretaryId,
                                  String ipAddress,
                                  String userAgent) {}

    public record JoinTeamResult(User doctor,
                                 DoctorProfile doctorProfile,
                                 User secretary,
                                 DoctorSecretary link,
                                 boolean newUserCreated,
                                 boolean linkUpdated) {}

    public record SecretaryListing(User secretary, Instant linkedAt) {}

    public record InviteSummary(String code,
                                TeamInviteStatus status,
                                int usesRemaining,
                                Instant expiresAt,
                                String secretaryFullName,
                                String secretaryCorporateEmail) {}

    public record DoctorListing(User doctor, DoctorProfile profile) {}
}
