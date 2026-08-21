package com.passmais.interfaces.dto.schedule;

import java.util.List;

public record PatientScheduleResponse(
        String doctorId,
        String doctorName,
        String doctorSpecialty,
        String doctorCrm,
        String timezone,
        String startDate,
        String endDate,
        List<PatientScheduleDayResponse> days
) {
}
