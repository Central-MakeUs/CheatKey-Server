package com.cheatkey.module.community.domain.repository;

import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import java.util.List;
import static com.cheatkey.module.community.domain.entity.QCommunityPost.communityPost;

@RequiredArgsConstructor
public class CommunityPostRepositoryImpl implements CommunityPostRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CommunityPost> findAllByCustomConditions(Long userId, String keyword, String sort, Pageable pageable) {
        BooleanExpression isActive = communityPost.status.eq(PostStatus.ACTIVE);
        BooleanExpression search = Expressions.TRUE;
        if (keyword != null && !keyword.isEmpty()) {
            search = communityPost.title.containsIgnoreCase(keyword)
                    .or(communityPost.content.containsIgnoreCase(keyword));
        }
        OrderSpecifier<?> order = communityPost.createdAt.desc();
        if ("popular".equals(sort)) {
            order = communityPost.viewCount.desc();
        }
        JPQLQuery<CommunityPost> query = queryFactory.selectFrom(communityPost)
                .where(isActive.and(search))
                .orderBy(order);
        long total = query.fetchCount();
        List<CommunityPost> content = query.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        return new PageImpl<>(content, pageable, total);
    }
} 