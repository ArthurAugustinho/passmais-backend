package com.passmais.application.service;

import com.passmais.domain.entity.PatientFile;
import com.passmais.domain.util.EmailUtils;
import com.passmais.infrastructure.repository.PatientFileRepository;
import com.passmais.infrastructure.validation.CPFValidator;
import com.passmais.interfaces.dto.PatientFileHealthInsuranceDTO;
import com.passmais.interfaces.dto.PatientFileResponsibleDTO;
import com.passmais.interfaces.dto.PatientFileUpsertRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class PatientFileService {

    private final PatientFileRepository patientFileRepository;

    public PatientFileService(PatientFileRepository patientFileRepository) {
        this.patientFileRepository = patientFileRepository;
    }

    public PatientFileUpsertResult upsert(PatientFileUpsertRequestDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da ficha são obrigatórios.");
        }

        String patientCpf = sanitizeDigits(requireText(dto.cpf(), "CPF do paciente é obrigatório."));

        PatientFile entity = patientFileRepository.findByCpf(patientCpf).orElse(null);
        boolean created = entity == null;
        if (created) {
            entity = new PatientFile();
            entity.setCpf(patientCpf);
        }

        entity.setFullName(requireText(dto.fullName(), "Nome completo é obrigatório."));
        entity.setBirthDate(requireDate(dto.birthDate(), "Data de nascimento é obrigatória."));

        if (dto.motherName() != null) {
            entity.setMotherName(normalizeText(dto.motherName()));
        }
        if (dto.sex() != null) {
            entity.setSex(dto.sex());
        }
        if (dto.email() != null) {
            entity.setEmail(normalizeEmail(dto.email()));
        }
        entity.setContactPhone(requireText(dto.contactPhone(), "Telefone de contato é obrigatório."));
        if (dto.fullAddress() != null) {
            entity.setFullAddress(normalizeText(dto.fullAddress()));
        }

        Boolean hasResponsible = dto.hasLegalResponsible();
        if (hasResponsible == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo 'hasLegalResponsible' é obrigatório.");
        }
        entity.setHasLegalResponsible(hasResponsible);
        applyResponsible(entity, hasResponsible, dto.responsible());

        if (dto.healthInsurance() != null) {
            applyHealthInsurance(entity, dto.healthInsurance());
        }

        Instant presenceConfirmedAt = dto.presenceConfirmedAt();
        if (presenceConfirmedAt == null) {
            presenceConfirmedAt = Instant.now();
        }
        entity.setPresenceConfirmedAt(presenceConfirmedAt);

        PatientFile saved = patientFileRepository.save(entity);
        return new PatientFileUpsertResult(saved, created);
    }

    public PatientFile findByCpf(String cpf) {
        String sanitizedCpf = sanitizeDigits(requireText(cpf, "CPF é obrigatório."));
        return patientFileRepository.findByCpf(sanitizedCpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma ficha encontrada para este CPF."));
    }

    private void applyResponsible(PatientFile entity, boolean hasResponsible, PatientFileResponsibleDTO dto) {
        if (!hasResponsible) {
            entity.setResponsibleFullName(null);
            entity.setResponsibleRelationship(null);
            entity.setResponsibleCpf(null);
            entity.setResponsiblePhone(null);
            return;
        }
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do responsável são obrigatórios.");
        }
        entity.setResponsibleFullName(requireText(dto.fullName(), "Nome do responsável é obrigatório."));
        entity.setResponsibleRelationship(requireText(dto.relationship(), "Parentesco do responsável é obrigatório."));
        String cpf = requireText(dto.cpf(), "CPF do responsável é obrigatório.");
        String sanitizedCpf = sanitizeDigits(cpf);
        if (!CPFValidator.isValid(sanitizedCpf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CPF do responsável é inválido.");
        }
        entity.setResponsibleCpf(sanitizedCpf);
        entity.setResponsiblePhone(requireText(dto.phone(), "Telefone do responsável é obrigatório."));
    }

    private void applyHealthInsurance(PatientFile entity, PatientFileHealthInsuranceDTO dto) {
        if (dto == null) {
            return;
        }
        entity.setHealthInsuranceName(normalizeText(dto.name()));
    }

    private String sanitizeDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return EmailUtils.normalize(email);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private java.time.LocalDate requireDate(java.time.LocalDate value, String message) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    public record PatientFileUpsertResult(PatientFile patientFile, boolean created) {}
}
