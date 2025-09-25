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

    // 차단된 댓글 DTO 변환
    default CommunityCommentResponse toBlockedCommentDto(CommunityComment comment, List<CommunityCommentResponse> children) {
        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .authorNickname("(차단된 사용자)")
                .content("차단한 사용자의 댓글입니다.")
                .status("BLOCKED_BY_USER")
                .createdAt(comment.getCreatedAt())
                .canDelete(false)
                .children(children)
                .build();
    }

    // 삭제된 댓글 DTO 변환
    default CommunityCommentResponse toDeletedCommentDto(CommunityComment comment, List<CommunityCommentResponse> children) {
        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .authorNickname("(삭제)")
                .content("삭제된 댓글입니다.")
                .status(CommentStatus.DELETED.name())
                .createdAt(comment.getCreatedAt())
                .canDelete(false)
                .children(children)
                .build();
    }

    // 신고된 댓글 DTO 변환
    default CommunityCommentResponse toReportedCommentDto(CommunityComment comment, List<CommunityCommentResponse> children) {
        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .authorNickname("(신고된 유저)")
                .content("관리자 규제된 댓글입니다.")
                .status(CommentStatus.REPORTED.name())
                .createdAt(comment.getCreatedAt())
                .canDelete(false)
                .children(children)
                .build();
    }

    // 정상 댓글 DTO 변환
    default CommunityCommentResponse toNormalCommentDto(CommunityComment comment, List<CommunityCommentResponse> children, Long currentUserId) {
        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .authorNickname(comment.getAuthorNickname())
                .content(comment.getContent())
                .status(comment.getStatus().name())
                .createdAt(comment.getCreatedAt())
                .canDelete(comment.getAuthorId().equals(currentUserId))
                .children(children)
                .build();
    }
} 