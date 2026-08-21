package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.PatientFile;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.infrastructure.repository.AppointmentRepository;
import com.passmais.infrastructure.repository.PatientFileRepository;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import com.passmais.interfaces.dto.PatientPresenceResponseDTO;
import com.passmais.interfaces.dto.PatientFileResponseDTO;
import com.passmais.interfaces.mapper.AppointmentMapper;
import com.passmais.interfaces.mapper.PatientFileResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientPresenceQueryService {

    private final AppointmentRepository appointmentRepository;
    private final PatientFileRepository patientFileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AppointmentMapper appointmentMapper;
    private final PatientFileResponseMapper patientFileResponseMapper;

    public PatientPresenceQueryService(AppointmentRepository appointmentRepository,
                                       PatientFileRepository patientFileRepository,
                                       PatientProfileRepository patientProfileRepository,
                                       AppointmentMapper appointmentMapper,
                                       PatientFileResponseMapper patientFileResponseMapper) {
        this.appointmentRepository = appointmentRepository;
        this.patientFileRepository = patientFileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.appointmentMapper = appointmentMapper;
        this.patientFileResponseMapper = patientFileResponseMapper;
    }

    public List<PatientPresenceResponseDTO> findConfirmedPresenceByDate(LocalDate date) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data é obrigatória.");
        }
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .minusNanos(1)
                .toInstant();

        List<Appointment> appointments = appointmentRepository.findByDateTimeBetween(startOfDay, endOfDay);
        if (appointments.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> cpfByUserId = resolveCpfsFromProfiles(appointments);
        Map<String, List<Appointment>> appointmentsByCpf = appointments.stream()
                .map(appt -> Map.entry(resolveCpf(appt, cpfByUserId), appt))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        if (appointmentsByCpf.isEmpty()) {
            return List.of();
        }

        Set<String> cpfs = appointmentsByCpf.keySet();
        List<PatientFile> files = patientFileRepository.findByCpfIn(cpfs);

        List<PatientPresenceResponseDTO> responses = new ArrayList<>();
        for (PatientFile file : files) {
            if (file.getPresenceConfirmedAt() == null) {
                continue;
            }
            String fileCpf = sanitizeDigits(file.getCpf());
            if (fileCpf == null) {
                continue;
            }
            List<Appointment> patientAppointments = appointmentsByCpf.get(fileCpf);
            if (patientAppointments == null || patientAppointments.isEmpty()) {
                continue;
            }
            PatientFileResponseDTO patientDto = patientFileResponseMapper.toResponse(file);
            patientAppointments.stream()
                    .sorted(Comparator.comparing(Appointment::getDateTime))
                    .map(appointmentMapper::toResponse)
                    .map(apptDto -> new PatientPresenceResponseDTO(patientDto, apptDto))
                    .forEach(responses::add);
        }
        return responses;
    }

    private String sanitizeDigits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private Map<UUID, String> resolveCpfsFromProfiles(List<Appointment> appointments) {
        Set<UUID> userIds = appointments.stream()
                .filter(appt -> sanitizeDigits(appt.getPatientCpf()) == null)
                .map(Appointment::getPatient)
                .filter(java.util.Objects::nonNull)
                .map(com.passmais.domain.entity.User::getId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return patientProfileRepository.findByUserIdIn(userIds).stream()
                .filter(profile -> profile.getCpf() != null)
                .collect(Collectors.toMap(
                        profile -> profile.getUser().getId(),
                        profile -> sanitizeDigits(profile.getCpf()),
                        (a, b) -> a
                ));
    }

    private String resolveCpf(Appointment appointment, Map<UUID, String> cpfByUserId) {
        String cpf = sanitizeDigits(appointment.getPatientCpf());
        if (cpf != null) {
            return cpf;
        }
        if (appointment.getPatient() != null) {
            String userCpf = sanitizeDigits(appointment.getPatient().getCpf());
            if (userCpf != null) {
                return userCpf;
            }
            return cpfByUserId.get(appointment.getPatient().getId());
        }
        return null;
    }
}
