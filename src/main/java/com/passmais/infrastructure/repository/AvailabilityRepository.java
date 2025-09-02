package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.Availability;
import com.passmais.domain.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {
    List<Availability> findByDoctorAndDayOfWeekOrderByStartTime(DoctorProfile doctor, DayOfWeek dayOfWeek);
    boolean existsByDoctorAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(DoctorProfile doctor, DayOfWeek dayOfWeek, LocalTime endExclusive, LocalTime startExclusive);
}

