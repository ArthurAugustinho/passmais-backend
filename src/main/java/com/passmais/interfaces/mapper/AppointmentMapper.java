package com.passmais.interfaces.mapper;

import com.passmais.domain.entity.Appointment;
import com.passmais.interfaces.dto.AppointmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    @Mapping(source = "doctor.id", target = "doctorId")
    @Mapping(source = "patient.id", target = "patientId")
    AppointmentResponseDTO toResponse(Appointment appt);
}

