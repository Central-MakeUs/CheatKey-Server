package com.cheatkey.module.home.domain.mapper;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.service.CommunityService.CommunityPostWithAuthorInfo;
import com.cheatkey.module.home.interfaces.dto.HomeDashboardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HomeMapper {

    @Mapping(target = "level", constant = "1") // 레벨은 1로 고정
    @Mapping(target = "totalVisitCount", ignore = true) // 파사드에서 실제 방문 횟수 설정
    @Mapping(target = "profileImageUrl", ignore = true) // 파사드에서 실제 URL 설정
    HomeDashboardResponse.UserInfo toUserInfo(Auth auth);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "content", source = "content") // NOTE: 프론트엔드에서 자동으로 텍스트 자르기 처리
    @Mapping(target = "authorNickname", source = "authorNickname")
    @Mapping(target = "authorProfileImageUrl", ignore = true) // 기존 메서드에서는 무시
    HomeDashboardResponse.PopularPost toPopularPost(CommunityPost post);

    List<HomeDashboardResponse.PopularPost> toPopularPosts(List<CommunityPost> posts);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "title", source = "post.title")
    @Mapping(target = "content", source = "post.content") // NOTE: 프론트엔드에서 자동으로 텍스트 자르기 처리
    @Mapping(target = "authorNickname", source = "post.authorNickname")
    @Mapping(target = "authorProfileImageUrl", ignore = true) // 파사드에서 실제 URL 설정
    HomeDashboardResponse.PopularPost toPopularPostWithAuthorInfo(CommunityPostWithAuthorInfo postWithAuthorInfo);

    List<HomeDashboardResponse.PopularPost> toPopularPostsWithAuthorInfo(List<CommunityPostWithAuthorInfo> postsWithAuthorInfo);
} 