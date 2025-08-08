package com.cheatkey.module.community.domain.entity.comment;

import com.cheatkey.module.community.domain.entity.CommunityPost;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_community_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CommunityComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommunityComment parent; // null이면 댓글, 아니면 대댓글

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 50)
    private String authorNickname;

    @Column(nullable = false, length = 200)
    private String content;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CommentStatus status;

    @Setter
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Setter
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = CommentStatus.ACTIVE;
    }
}
