package com.passmais.interfaces.controller;

import com.passmais.domain.entity.ModerationLog;
import com.passmais.domain.entity.Review;
import com.passmais.domain.enums.ReviewStatus;
import com.passmais.infrastructure.repository.ModerationLogRepository;
import com.passmais.infrastructure.repository.ReviewRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reviews")
public class ReviewModerationController {

    private final ReviewRepository reviewRepository;
    private final ModerationLogRepository moderationLogRepository;

    public ReviewModerationController(ReviewRepository reviewRepository, ModerationLogRepository moderationLogRepository) {
        this.reviewRepository = reviewRepository;
        this.moderationLogRepository = moderationLogRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @GetMapping
    public ResponseEntity<List<Review>> listAll() {
        return ResponseEntity.ok(reviewRepository.findAll());
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @PostMapping("/{id}/status")
    public ResponseEntity<Review> setStatus(@PathVariable UUID id, @RequestParam @NotNull ReviewStatus status, @RequestParam(name = "adminUserId") UUID adminUserId) {
        Review r = reviewRepository.findById(id).orElseThrow();
        r.setStatus(status);
        Review saved = reviewRepository.save(r);
        ModerationLog log = ModerationLog.builder()
                .adminUserId(adminUserId)
                .action("REVIEW_" + status.name())
                .details("Review " + id + " set to " + status)
                .build();
        moderationLogRepository.save(log);
        return ResponseEntity.ok(saved);
    }
}

