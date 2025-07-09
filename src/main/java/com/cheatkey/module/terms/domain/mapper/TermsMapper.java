package com.cheatkey.module.terms.domain.mapper;

import com.cheatkey.common.config.mapper.MapStructMapperConfig;
import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.interfaces.dto.TermsDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", config = MapStructMapperConfig.class)
public interface TermsMapper {
    List<TermsDto> toDtoList(List<Terms> termsList);
}