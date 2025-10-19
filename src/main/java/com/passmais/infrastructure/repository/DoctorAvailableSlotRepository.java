package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorAvailableSlot;
import com.passmais.domain.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DoctorAvailableSlotRepository extends JpaRepository<DoctorAvailableSlot, UUID> {
    List<DoctorAvailableSlot> findByDoctorAndSlotDateBetweenOrderByStartAtUtc(DoctorProfile doctor, LocalDate start, LocalDate end);

    @Modifying
    @Query("delete from DoctorAvailableSlot s where s.doctor = :doctor and s.slotDate >= :start")
    void deleteFutureSlotsFrom(@Param("doctor") DoctorProfile doctor, @Param("start") LocalDate start);
}
