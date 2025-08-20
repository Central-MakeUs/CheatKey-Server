package com.cheatkey.module.community.domain.service;

import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 탈퇴한 사용자 ID 목록을 캐싱하여 성능을 최적화하는 서비스
 *
 * 탈퇴한 사용자 정보는 자주 변경되지 않으므로 캐싱이 효과적입니다.
 * 회원 탈퇴 시에만 캐시를 무효화하여 최신 정보를 유지합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnUserCacheService {

    private final AuthRepository authRepository;

    /**
     * 탈퇴한 사용자 ID 목록을 조회합니다.
     *
     * @return 탈퇴한 사용자 ID 목록
     */
    @Cacheable(value = "withdrawnUsers", key = "'all'")
    public List<Long> getWithdrawnUserIds() {
        log.debug("탈퇴한 사용자 ID 목록을 데이터베이스에서 조회합니다.");
        return authRepository.findIdsByStatus(AuthStatus.WITHDRAWN);
    }

    /**
     * 특정 사용자가 탈퇴했는지 확인합니다.
     *
     * @param userId 확인할 사용자 ID
     * @return 탈퇴한 사용자 여부
     */
    public boolean isWithdrawnUser(Long userId) {
        if (userId == null) return false;
        return getWithdrawnUserIds().contains(userId);
    }

    /**
     * 탈퇴한 사용자 캐시를 무효화합니다.
     * 회원 탈퇴 시 호출되어야 합니다.
     */
    @CacheEvict(value = "withdrawnUsers", key = "'all'")
    public void evictWithdrawnUsersCache() {
        log.debug("탈퇴한 사용자 캐시를 무효화합니다.");
    }
}
