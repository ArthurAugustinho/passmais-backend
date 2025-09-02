package com.passmais.interfaces.mapper;

import com.passmais.domain.entity.User;
import com.passmais.interfaces.dto.UserCreateDTO;
import com.passmais.interfaces.dto.UserResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "lgpdAcceptedAt", expression = "java(dto.lgpdAccepted() ? java.time.Instant.now() : null)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "accountLockedUntil", ignore = true)
    @Mapping(target = "lastTokenRevalidatedAt", ignore = true)
    User toEntity(UserCreateDTO dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "role", source = "role")
    UserResponseDTO toResponse(User entity);
}
