package com.passmais.application.service;

import com.passmais.domain.entity.Appointment;
import com.passmais.domain.entity.Review;
import com.passmais.domain.enums.AppointmentStatus;
import com.passmais.domain.enums.ReviewStatus;
import com.passmais.infrastructure.repository.ReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public Review createFromAppointment(Appointment appt, int rating, String comment) {
        if (appt.getStatus() != AppointmentStatus.DONE) {
            throw new IllegalArgumentException("Avaliação permitida somente após comparecimento");
        }
        Review r = Review.builder()
                .appointment(appt)
                .rating(rating)
                .comment(comment)
                .status(ReviewStatus.PENDING)
                .build();
        return reviewRepository.save(r);
    }
}

