package com.passmais.interfaces.mapper;

import com.passmais.domain.entity.PatientFile;
import com.passmais.interfaces.dto.PatientFileHealthInsuranceDTO;
import com.passmais.interfaces.dto.PatientFileResponseDTO;
import com.passmais.interfaces.dto.PatientFileResponsibleDTO;
import org.springframework.stereotype.Component;

@Component
public class PatientFileResponseMapper {

    public PatientFileResponseDTO toResponse(PatientFile entity) {
        if (entity == null) {
            return null;
        }

        PatientFileResponsibleDTO responsible = null;
        if (Boolean.TRUE.equals(entity.getHasLegalResponsible())) {
            responsible = new PatientFileResponsibleDTO(
                    entity.getResponsibleFullName(),
                    entity.getResponsibleRelationship(),
                    entity.getResponsibleCpf(),
                    entity.getResponsiblePhone()
            );
        }

        PatientFileHealthInsuranceDTO healthInsurance = null;
        if (entity.getHealthInsuranceName() != null) {
            healthInsurance = new PatientFileHealthInsuranceDTO(entity.getHealthInsuranceName());
        }

        return new PatientFileResponseDTO(
                entity.getFullName(),
                entity.getCpf(),
                entity.getMotherName(),
                entity.getSex(),
                entity.getEmail(),
                entity.getContactPhone(),
                entity.getFullAddress(),
                entity.getHasLegalResponsible(),
                responsible,
                healthInsurance,
                entity.getPresenceConfirmedAt()
        );
    }
}
