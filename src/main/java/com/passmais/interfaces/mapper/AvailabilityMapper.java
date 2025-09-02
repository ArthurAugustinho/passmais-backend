package com.passmais.interfaces.mapper;

import com.passmais.domain.entity.Availability;
import com.passmais.interfaces.dto.AvailabilityCreateDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AvailabilityMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "dayOfWeek", source = "dayOfWeek")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    Availability toEntity(AvailabilityCreateDTO dto);
}
