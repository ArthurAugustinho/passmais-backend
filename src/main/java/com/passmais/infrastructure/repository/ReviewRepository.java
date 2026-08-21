package com.passmais.infrastructure.repository;

import com.passmais.domain.entity.Review;
import com.passmais.domain.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    @Query("select avg(r.rating) from Review r where r.appointment.doctor = :doctor and r.status = 'APPROVED'")
    Optional<Double> averageForDoctor(@Param("doctor") DoctorProfile doctor);

    @Query("select count(r) from Review r where r.appointment.doctor = :doctor and r.status = 'APPROVED'")
    long countApprovedForDoctor(@Param("doctor") DoctorProfile doctor);
}
