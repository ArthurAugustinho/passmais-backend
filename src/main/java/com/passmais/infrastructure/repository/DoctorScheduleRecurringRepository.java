package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.entity.DoctorScheduleRecurring;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorScheduleRecurringRepository extends JpaRepository<DoctorScheduleRecurring, UUID> {
    List<DoctorScheduleRecurring> findByDoctor(DoctorProfile doctor);
    Optional<DoctorScheduleRecurring> findByDoctorAndWeekday(DoctorProfile doctor, DayOfWeek weekday);
    void deleteByDoctor(DoctorProfile doctor);
}
