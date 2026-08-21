package com.passmais.interfaces.controller;

import com.passmais.domain.entity.Notification;
import com.passmais.domain.entity.User;
import com.passmais.infrastructure.repository.NotificationRepository;
import com.passmais.infrastructure.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMINISTRATOR')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> listByUser(@PathVariable UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Notification> list = notificationRepository.findAll().stream().filter(n -> n.getUser().getId().equals(user.getId())).toList();
        return ResponseEntity.ok(list);
    }
}
