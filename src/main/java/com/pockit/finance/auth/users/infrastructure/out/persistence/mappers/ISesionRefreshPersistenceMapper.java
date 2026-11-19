package com.pockit.finance.auth.users.infrastructure.out.persistence.mappers;

import com.pockit.finance.auth.users.domain.models.SesionRefresh;
import com.pockit.finance.auth.users.infrastructure.out.persistence.entities.SesionRefreshEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ISesionRefreshPersistenceMapper {

    SesionRefresh toDomain(SesionRefreshEntity entity);

    SesionRefreshEntity toEntity(SesionRefresh domain);
}
