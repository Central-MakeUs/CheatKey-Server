package com.cheatkey.module.community.domian.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;



@Table(name = "t_community_posts")
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommunityPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    @Enumerated(EnumType.STRING)
    private CommunityCategory category;

    private Long userId;
    private String nickname;
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private PostStatus status; // ACTIVE, DELETED, REPORTED (soft delete)


    public static CommunityPost createPost(String title, String content, CommunityCategory category, Long userId, String nickname) {
        return CommunityPost.builder()
                .title(title)
                .content(content)
                .category(category)
                .userId(userId)
                .nickname(nickname)
                .viewCount(0L)
                .status(PostStatus.ACTIVE)
                .build();
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }
}
