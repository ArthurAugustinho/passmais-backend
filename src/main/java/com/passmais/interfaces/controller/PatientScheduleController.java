package com.passmais.interfaces.controller;

import com.passmais.application.service.PatientScheduleService;
import com.passmais.application.service.exception.ScheduleException;
import com.passmais.interfaces.dto.schedule.PatientScheduleResponse;
import com.passmais.interfaces.dto.schedule.ScheduleErrorResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/patient")
public class PatientScheduleController {

    private final PatientScheduleService patientScheduleService;

    public PatientScheduleController(PatientScheduleService patientScheduleService) {
        this.patientScheduleService = patientScheduleService;
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/doctors/{doctorId}/schedule")
    public ResponseEntity<?> getDoctorSchedule(@PathVariable UUID doctorId,
                                               @RequestParam(name = "startDate", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                               @RequestParam(name = "endDate", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            PatientScheduleResponse response = patientScheduleService.getDoctorSchedule(doctorId, startDate, endDate);
            return ResponseEntity.ok(response);
        } catch (ScheduleException ex) {
            ScheduleErrorResponse error = new ScheduleErrorResponse(
                    "error",
                    ex.getCode(),
                    ex.getMessage(),
                    ex.getDetails()
            );
            return ResponseEntity.status(ex.getStatus()).body(error);
        }
    }
}
