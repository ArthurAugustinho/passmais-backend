package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorSecretary;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.DoctorProfileRepository;
import com.passmais.infrastructure.repository.DoctorSecretaryRepository;
import com.passmais.interfaces.dto.SecretaryAppointmentPageResponseDTO;
import com.passmais.interfaces.dto.SecretaryAppointmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SecretaryAppointmentService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorSecretaryRepository doctorSecretaryRepository;

    public SecretaryAppointmentService(AppointmentRepository appointmentRepository,
                                       DoctorProfileRepository doctorProfileRepository,
                                       DoctorSecretaryRepository doctorSecretaryRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorSecretaryRepository = doctorSecretaryRepository;
    }

    @Transactional(readOnly = true)
    public SecretaryAppointmentPageResponseDTO listAppointmentsForDoctor(UUID doctorId,
                                                                         Instant from,
                                                                         Instant to,
                                                                         List<AppointmentStatus> statuses,
                                                                         Integer page,
                                                                         Integer size,
                                                                         Sort sort,
                                                                         UUID secretaryUserId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médico não encontrado"));
        if (doctor.getUser() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Médico sem usuário associado");
        }

        DoctorSecretary link = doctorSecretaryRepository.findByIdDoctorIdAndIdSecretaryId(
                        doctor.getUser().getId(),
                        secretaryUserId
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Secretária não vinculada a este médico"));
        if (!link.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vínculo com o médico está inativo");
        }

        int resolvedPage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int resolvedSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : size;
        Sort resolvedSort = (sort == null) ? Sort.by(Sort.Direction.ASC, "dateTime") : sort;

        Specification<Appointment> spec = Specification.where(byDoctor(doctorId));
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateTime"), to));
        }
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(statuses));
        }

        Pageable pageable = PageRequest.of(resolvedPage - 1, resolvedSize, resolvedSort);
        Page<Appointment> pageResult = appointmentRepository.findAll(spec, pageable);

        List<SecretaryAppointmentResponseDTO> items = pageResult.getContent().stream()
                .map(this::toDto)
                .toList();

        return new SecretaryAppointmentPageResponseDTO(
                items,
                resolvedPage,
                resolvedSize,
                pageResult.getTotalElements()
        );
    }

    private Specification<Appointment> byDoctor(UUID doctorId) {
        return (root, query, cb) -> cb.equal(root.get("doctor").get("id"), doctorId);
    }

    private SecretaryAppointmentResponseDTO toDto(Appointment appointment) {
        return new SecretaryAppointmentResponseDTO(
                appointment.getDoctor() != null && appointment.getDoctor().getUser() != null
                        ? appointment.getDoctor().getUser().getName()
                        : null,
                appointment.getDoctor() != null ? appointment.getDoctor().getCrm() : null,
                appointment.getDependent() != null ? appointment.getDependent().getId() : null,
                appointment.getDateTime(),
                appointment.getObservations(),
                appointment.getReason(),
                appointment.getSymptomDuration(),
                appointment.getPreConsultNotes(),
                appointment.getRescheduledFrom() != null ? appointment.getRescheduledFrom().getId() : null,
                appointment.getStatus(),
                appointment.getValue(),
                appointment.getBookedAt(),
                resolvePatientFullName(appointment),
                appointment.getPatientCpf(),
                appointment.getPatientBirthDate(),
                appointment.getPatientCellPhone(),
                appointment.getLocation(),
                appointment.getCreatedAt(),
                appointment.getFinalizedAt()
        );
    }

    private String resolvePatientFullName(Appointment appointment) {
        if (appointment.getPatientFullName() != null && !appointment.getPatientFullName().isBlank()) {
            return appointment.getPatientFullName();
        }
        if (appointment.getPatient() != null && appointment.getPatient().getName() != null) {
            return appointment.getPatient().getName();
        }
        if (appointment.getDependent() != null && appointment.getDependent().getName() != null) {
            return appointment.getDependent().getName();
        }
        return null;
    }
}
