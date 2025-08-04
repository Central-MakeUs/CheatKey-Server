package com.cheatkey.module.terms.domain.mapper;

import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.interfaces.dto.TermsResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TermsMapper {
    List<TermsResponse> toDtoList(List<Terms> termsList);
}