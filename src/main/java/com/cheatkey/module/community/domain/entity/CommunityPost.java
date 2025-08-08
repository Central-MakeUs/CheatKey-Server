package com.cheatkey.module.community.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    private Long authorId;
    private String authorNickname;
    private Long viewCount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private PostStatus status; // ACTIVE, DELETED, REPORTED (soft delete)


    public static CommunityPost createPost(String title, String content, CommunityCategory category, Long authorId, String authorNickname) {
        return CommunityPost.builder()
                .title(title)
                .content(content)
                .category(category)
                .authorId(authorId)
                .authorNickname(authorNickname)
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
