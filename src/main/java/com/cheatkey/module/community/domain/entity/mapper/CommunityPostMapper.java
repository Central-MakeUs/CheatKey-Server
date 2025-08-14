package com.cheatkey.module.community.domain.entity.mapper;

import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.comment.CommunityComment;
import com.cheatkey.module.community.domain.entity.comment.CommentStatus;
import com.cheatkey.module.community.interfaces.dto.CommunityPostListResponse;
import com.cheatkey.module.community.interfaces.dto.CommunityPostDetailResponse;
import com.cheatkey.module.community.interfaces.dto.comment.CommunityCommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Mapper(componentModel = "spring")
public interface CommunityPostMapper {
    CommunityPostMapper INSTANCE = Mappers.getMapper(CommunityPostMapper.class);

    @Mapping(target = "thumbnailUrls", source = "thumbnailUrls")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(source = "post.authorNickname", target = "authorNickname")
    @Mapping(source = "post.content", target = "content")
    CommunityPostListResponse toListDto(CommunityPost post, int commentCount, List<String> thumbnailUrls);

    @Mapping(target = "presignedUrls", source = "presignedUrls")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "canDelete", source = "canDelete")
    @Mapping(target = "blocked", source = "blocked")
    @Mapping(target = "blockMessage", source = "blockMessage")
    @Mapping(source = "post.authorNickname", target = "authorNickname")
    CommunityPostDetailResponse toDetailDto(
        CommunityPost post,
        int commentCount,
        List<String> presignedUrls,
        List<CommunityCommentResponse> comments,
        boolean canDelete,
        boolean blocked,
        String blockMessage
    );

    default List<CommunityCommentResponse> toCommentDtoList(List<CommunityComment> comments, Long currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        // 댓글/대댓글 트리 구성
        Map<Long, List<CommunityComment>> childrenMap = comments.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // 부모 댓글과 고아 대댓글(삭제된 부모 댓글의 자식)을 모두 포함

        // 1. 부모 댓글들 추가
        List<CommunityCommentResponse> parentComments = comments.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> toCommentDtoWithChildren(c, childrenMap, currentUserId))
                .toList();
        List<CommunityCommentResponse> result = new ArrayList<>(parentComments);
        
        // 2. 고아 대댓글들 추가 (삭제된 부모 댓글의 자식)
        List<CommunityCommentResponse> orphanReplies = comments.stream()
                .filter(c -> c.getParent() != null && 
                       comments.stream().noneMatch(p -> p.getId().equals(c.getParent().getId()) && 
                                                      p.getStatus() == CommentStatus.ACTIVE))
                .map(c -> toCommentDtoWithChildren(c, childrenMap, currentUserId))
                .toList();
        result.addAll(orphanReplies);
        
        return result;
    }

    default CommunityCommentResponse toCommentDtoWithChildren(CommunityComment comment, Map<Long, List<CommunityComment>> childrenMap, Long currentUserId) {
        List<CommunityCommentResponse> children = childrenMap.getOrDefault(comment.getId(), List.of()).stream()
                .map(child -> toCommentDtoWithChildren(child, childrenMap, currentUserId))
                .collect(Collectors.toList());

        boolean canDelete = comment.getAuthorId().equals(currentUserId);

        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .authorNickname(comment.getAuthorNickname())
                .content(comment.getContent())
                .status(comment.getStatus().name())
                .createdAt(comment.getCreatedAt())
                .canDelete(canDelete)
                .children(children)
                .build();
    }
} 