package com.cheatkey.module.community.domian.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {
    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String content;

    @Enumerated(EnumType.STRING)
    private CommunityCategory category;

    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String imageUrls; // 콤마(,)로 구분된 S3 URL 리스트 (최대 5개)

    @Enumerated(EnumType.STRING)
    private PostStatus status; // ACTIVE, DELETED (soft delete)


    public static CommunityPost createPost(CommunityPost communityPost) {
        return CommunityPost.builder()
                .title(communityPost.title)
                .content(communityPost.content)
                .category(communityPost.category)
                .userId(communityPost.userId)
                .imageUrls(String.join(",", communityPost.imageUrls))
                .status(PostStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
