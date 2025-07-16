package com.cheatkey.module.community.domian.mapper;

import com.cheatkey.common.config.mapper.MapStructMapperConfig;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.interfaces.dto.CommunityPostCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", config = MapStructMapperConfig.class)
public interface CommunityMapper {

    @Mapping(target = "imageUrls", source = "imageUrls")
    CommunityPost toCommunityPost(CommunityPostCreateRequest request);

    default String map(List<String> imageUrls) {
        return imageUrls == null ? null : String.join(",", imageUrls);
    }
}
