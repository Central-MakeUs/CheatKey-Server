package com.cheatkey.module.community.domian.entity.mapper;

import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.interfaces.dto.CommunityPostListResponse;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import com.cheatkey.module.file.interfaces.dto.FileUploadResponse;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CommunityPostMapper {
    CommunityPostMapper INSTANCE = Mappers.getMapper(CommunityPostMapper.class);

    @Mapping(target = "thumbnailUrls", source = "thumbnailUrls")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(source = "post.nickname", target = "authorNickname")
    CommunityPostListResponse toListDto(CommunityPost post, int commentCount, List<String> thumbnailUrls);

    @Mapping(target = "files", source = "files")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "canDelete", source = "canDelete")
    @Mapping(target = "blocked", source = "blocked")
    @Mapping(target = "blockMessage", source = "blockMessage")
    @Mapping(source = "post.nickname", target = "authorNickname")
    CommunityPostDetailResponse toDetailDto(
        CommunityPost post,
        int commentCount,
        List<FileUploadResponse> files,
        List<CommunityCommentResponse> comments,
        boolean canDelete,
        boolean blocked,
        String blockMessage
    );
} 