package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.DoctorScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorScheduleSlotRepository extends JpaRepository<DoctorScheduleSlot, UUID> {

    @Query("select coalesce(max(s.version), 0) from DoctorScheduleSlot s where s.doctor.id = :doctorId and s.date = :date")
    int findMaxVersion(@Param("doctorId") UUID doctorId, @Param("date") LocalDate date);

    @Query("select s from DoctorScheduleSlot s where s.doctor.id = :doctorId and s.date = :date and s.deletedAt is null")
    List<DoctorScheduleSlot> findActiveByDoctorAndDate(@Param("doctorId") UUID doctorId,
                                                       @Param("date") LocalDate date);

    @Modifying
    @Query("update DoctorScheduleSlot s set s.deletedAt = :deletedAt, s.deletedBy = :deletedBy, s.deleteReason = :reason where s.doctor.id = :doctorId and s.date = :date and s.deletedAt is null")
    int softDeleteActiveByDate(@Param("doctorId") UUID doctorId,
                               @Param("date") LocalDate date,
                               @Param("deletedAt") Instant deletedAt,
                               @Param("deletedBy") UUID deletedBy,
                               @Param("reason") String reason);

    @Query("select s from DoctorScheduleSlot s where s.doctor.id = :doctorId and s.date between :start and :end and s.deletedAt is null order by s.date asc, s.time asc")
    List<DoctorScheduleSlot> findActiveByDoctorAndDateRange(@Param("doctorId") UUID doctorId,
                                                            @Param("start") LocalDate start,
                                                            @Param("end") LocalDate end);
}
