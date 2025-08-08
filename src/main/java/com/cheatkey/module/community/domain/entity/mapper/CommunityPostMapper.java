package com.cheatkey.module.community.domain.entity.mapper;

import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.interfaces.dto.CommunityPostListResponse;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import com.cheatkey.module.file.interfaces.dto.FileUploadResponse;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CommunityPostMapper {
    CommunityPostMapper INSTANCE = Mappers.getMapper(CommunityPostMapper.class);

    @Mapping(target = "thumbnailUrls", source = "thumbnailUrls")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(source = "post.authorNickname", target = "authorNickname")
    CommunityPostListResponse toListDto(CommunityPost post, int commentCount, List<String> thumbnailUrls);

    @Mapping(target = "files", source = "files")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "canDelete", source = "canDelete")
    @Mapping(target = "blocked", source = "blocked")
    @Mapping(target = "blockMessage", source = "blockMessage")
    @Mapping(source = "post.authorNickname", target = "authorNickname")
    CommunityPostDetailResponse toDetailDto(
        CommunityPost post,
        int commentCount,
        List<FileUploadResponse> files,
        List<CommunityCommentResponse> comments,
        boolean canDelete,
        boolean blocked,
        String blockMessage
    );

    default List<CommunityCommentResponse> toCommentDtoList(List<CommunityComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        // 댓글/대댓글 트리 구성
        Map<Long, List<CommunityComment>> childrenMap = comments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return comments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> toCommentDtoWithChildren(c, childrenMap))
                .collect(Collectors.toList());
    }

    default CommunityCommentResponse toCommentDtoWithChildren(CommunityComment comment, Map<Long, List<CommunityComment>> childrenMap) {
        List<CommunityCommentResponse> children = childrenMap.getOrDefault(comment.getId(), List.of()).stream()
                .map(child -> toCommentDtoWithChildren(child, childrenMap))
                .collect(Collectors.toList());

        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .authorId(comment.getAuthorId())
                .authorNickname(comment.getAuthorNickname())
                .content(comment.getContent())
                .status(comment.getStatus().name())
                .createdAt(comment.getCreatedAt())
                .children(children)
                .build();
    }
} 