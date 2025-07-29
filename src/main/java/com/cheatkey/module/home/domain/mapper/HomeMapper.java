package com.cheatkey.module.home.domain.mapper;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.home.interfaces.dto.HomeDashboardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomeMapper {

    @Mapping(target = "level", constant = "1") // 레벨은 1로 고정
    @Mapping(target = "totalVisitCount", constant = "10") // @TODO: 해당 구현은 아직 안되어 있음으로 나중에 추가 되어야 한다
    @Mapping(target = "profileImageUrl", ignore = true) // 파사드에서 실제 URL 설정
    HomeDashboardResponse.UserInfo toUserInfo(Auth auth);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "content", source = "content", qualifiedByName = "truncateContent") // TODO: 프론트엔드에서 처리하는지 확인 필요
    @Mapping(target = "authorNickname", source = "nickname")
    HomeDashboardResponse.PopularPost toPopularPost(CommunityPost post);

    List<HomeDashboardResponse.PopularPost> toPopularPosts(List<CommunityPost> posts);


    @Named("truncateContent")
    default String truncateContent(String content) {
        // TODO: 프론트엔드에서 텍스트 자르기 처리하는지 확인 필요
        // 현재는 백엔드에서 100자로 제한하고 "..." 추가
        if (content == null || content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100) + "...";
    }
} 