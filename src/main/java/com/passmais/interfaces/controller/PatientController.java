package com.passmais.interfaces.controller;

import com.passmais.application.service.PatientFileService;
import com.passmais.application.service.PatientFileService.PatientFileUpsertResult;
import com.passmais.application.service.PatientPresenceQueryService;
import com.passmais.domain.entity.PatientProfile;
import com.passmais.infrastructure.repository.PatientProfileRepository;
import com.passmais.interfaces.dto.PatientFileResponseDTO;
import com.passmais.interfaces.dto.PatientFileUpsertRequestDTO;
import com.passmais.interfaces.dto.PatientFileUpsertResponseDTO;
import com.passmais.interfaces.dto.PatientPresenceResponseDTO;
import com.passmais.interfaces.mapper.PatientFileResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientProfileRepository patientRepo;
    private final PatientFileService patientFileService;
    private final PatientFileResponseMapper patientFileResponseMapper;
    private final PatientPresenceQueryService patientPresenceQueryService;

    public PatientController(PatientProfileRepository patientRepo,
                             PatientFileService patientFileService,
                             PatientFileResponseMapper patientFileResponseMapper,
                             PatientPresenceQueryService patientPresenceQueryService) {
        this.patientRepo = patientRepo;
        this.patientFileService = patientFileService;
        this.patientFileResponseMapper = patientFileResponseMapper;
        this.patientPresenceQueryService = patientPresenceQueryService;
    }

    @PreAuthorize("hasAnyRole('SECRETARY','ADMIN','ADMINISTRATOR','SUPERADMIN','DOCTOR')")
    @PostMapping
    public ResponseEntity<PatientFileUpsertResponseDTO> upsertPatientFile(@RequestBody @Valid PatientFileUpsertRequestDTO dto) {
        PatientFileUpsertResult result = patientFileService.upsert(dto);
        String message = result.created() ? "Paciente cadastrado com sucesso." : "Dados do paciente atualizados com sucesso.";
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(new PatientFileUpsertResponseDTO(result.patientFile().getId(), message));
    }

    @PreAuthorize("hasAnyRole('SECRETARY','ADMIN','ADMINISTRATOR','SUPERADMIN')")
    @GetMapping("/{cpf}")
    public ResponseEntity<PatientFileResponseDTO> getFileByCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(patientFileResponseMapper.toResponse(patientFileService.findByCpf(cpf)));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    @GetMapping("/file/{cpf}")
    public ResponseEntity<PatientFileResponseDTO> getFileForDoctor(@PathVariable String cpf) {
        return ResponseEntity.ok(patientFileResponseMapper.toResponse(patientFileService.findByCpf(cpf)));
    }

    @PreAuthorize("hasAnyRole('SECRETARY','DOCTOR')")
    @GetMapping("/presence")
    public ResponseEntity<java.util.List<PatientPresenceResponseDTO>> listPresenceByDate(@RequestParam("date") String dateParam) {
        LocalDate date = parseDate(dateParam);
        return ResponseEntity.ok(patientPresenceQueryService.findConfirmedPresenceByDate(date));
    }

    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<PatientProfile> get(@PathVariable UUID id) {
        return ResponseEntity.ok(patientRepo.findById(id).orElseThrow());
    }

    @PreAuthorize("hasRole('PATIENT')")
    @PutMapping("/{id:[0-9a-fA-F\\-]{36}}")
    public ResponseEntity<PatientProfile> update(@PathVariable UUID id,
                                                 @RequestParam(name = "cpf", required = false) String cpf,
                                                 @RequestParam(name = "birthDate", required = false) String birthDate,
                                                 @RequestParam(name = "address", required = false) String address,
                                                 @RequestParam(name = "cellPhone", required = false) String cellPhone,
                                                 @RequestParam(name = "communicationPreference", required = false) String communicationPreference) {
        PatientProfile p = patientRepo.findById(id).orElseThrow();
        if (cpf != null) p.setCpf(cpf);
        if (birthDate != null) p.setBirthDate(LocalDate.parse(birthDate));
        if (address != null) p.setAddress(address);
        if (cellPhone != null) p.setCellPhone(cellPhone);
        if (communicationPreference != null) p.setCommunicationPreference(communicationPreference);
        return ResponseEntity.ok(patientRepo.save(p));
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parâmetro 'date' é obrigatório.");
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de data inválido. Use yyyy-MM-dd.");
        }
    }
}
