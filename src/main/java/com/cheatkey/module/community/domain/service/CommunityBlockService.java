package com.cheatkey.module.community.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.community.domain.entity.CommunityPostBlock;
import com.cheatkey.module.community.domain.repository.CommunityPostBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 커뮤니티 사용자 차단 관리 서비스
 * 게시글, 댓글 등 커뮤니티 내 모든 차단 기능을 담당
 */
@Service
@RequiredArgsConstructor
public class CommunityBlockService {
    private final CommunityPostBlockRepository communityPostBlockRepository;
    
    /**
     * 사용자 차단
     * @param blockerId 차단하는 사용자 ID
     * @param blockedId 차단당하는 사용자 ID
     * @param reason 차단 사유
     */
    @Transactional
    public void blockUser(Long blockerId, Long blockedId, String reason) {
        // 1. 자기 자신을 차단하려는 경우 방지
        if (blockerId.equals(blockedId)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }
        
        // 2. 이미 차단한 유저인지 확인
        if (communityPostBlockRepository.existsByBlockerIdAndBlockedIdAndIsActiveTrue(blockerId, blockedId)) {
            throw new CustomException(ErrorCode.USER_ALREADY_BLOCKED);
        }
        
        // 3. 차단 처리
        CommunityPostBlock block = CommunityPostBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .reason(reason)
                .isActive(true)
                .build();
        communityPostBlockRepository.save(block);
    }
    
    /**
     * 차단된 사용자 ID 목록 조회
     * @param blockerId 차단하는 사용자 ID
     * @return 차단된 사용자 ID 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long blockerId) {
        return communityPostBlockRepository
            .findByBlockerIdAndIsActive(blockerId, true)
            .stream()
            .map(CommunityPostBlock::getBlockedId)
            .toList();
    }
    
    /**
     * 차단 여부 확인
     * @param blockerId 차단하는 사용자 ID
     * @param blockedId 확인할 사용자 ID
     * @return 차단 여부
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return communityPostBlockRepository
            .existsByBlockerIdAndBlockedIdAndIsActiveTrue(blockerId, blockedId);
    }
}

