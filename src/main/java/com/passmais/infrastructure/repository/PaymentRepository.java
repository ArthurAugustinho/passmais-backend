package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.Payment;
import com.passmais.domain.entity.DoctorProfile;
import com.passmais.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Query("select p from Payment p where p.appointment.doctor = :doctor and (:status is null or p.status = :status)")
    List<Payment> findByDoctorAndStatus(@Param("doctor") DoctorProfile doctor, @Param("status") PaymentStatus status);

    @Query("select coalesce(sum(p.value), 0) from Payment p where p.appointment.doctor = :doctor and p.status = 'PENDING'")
    BigDecimal sumPendingByDoctor(@Param("doctor") DoctorProfile doctor);
}
