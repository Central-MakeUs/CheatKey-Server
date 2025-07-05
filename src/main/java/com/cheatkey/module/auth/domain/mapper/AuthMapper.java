package com.cheatkey.module.auth.domain.mapper;

import com.cheatkey.common.config.mapper.MapStructMapperConfig;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", config = MapStructMapperConfig.class)
public interface AuthMapper {

    @Mapping(target = "tradeMethodCode", expression = "java(joinCodes(request.getTradeMethodCodeList()))")
    @Mapping(target = "tradeItemCode", expression = "java(joinCodes(request.getTradeItemCodeList()))")
    Auth toAuth(AuthRegisterRequest request);

    default String joinCodes(List<String> codes) {
        return (codes == null || codes.isEmpty()) ? null : String.join(",", codes);
    }
}
