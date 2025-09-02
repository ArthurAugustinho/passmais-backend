package com.passmais.application.service;

import com.passmais.domain.entity.Availability;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.infrastructure.repository.AvailabilityRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public AvailabilityService(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    public Availability createAvailability(DoctorProfile doctor, DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
        if (start.isAfter(end) || start.equals(end)) {
            throw new IllegalArgumentException("Horário inicial deve ser antes do final");
        }
        // Apenas disponibilidade futura: dia da semana igual mas data futura mínima da próxima ocorrência
        LocalDate today = LocalDate.now();
        if (dayOfWeek.equals(today.getDayOfWeek()) && start.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Disponibilidade deve ser futura");
        }
        // Blocos de 35 min
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes % 35 != 0) {
            throw new IllegalArgumentException("Disponibilidade deve ser múltipla de 35 minutos");
        }
        // Sem sobreposição
        boolean overlaps = availabilityRepository.existsByDoctorAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                doctor, dayOfWeek, end, start);
        if (overlaps) {
            throw new IllegalArgumentException("Disponibilidade sobreposta a outra existente");
        }

        Availability availability = Availability.builder()
                .doctor(doctor)
                .dayOfWeek(dayOfWeek)
                .startTime(start)
                .endTime(end)
                .build();
        return availabilityRepository.save(availability);
    }

    public List<Availability> listByDay(DoctorProfile doctor, DayOfWeek dayOfWeek) {
        return availabilityRepository.findByDoctorAndDayOfWeekOrderByStartTime(doctor, dayOfWeek);
    }
}

