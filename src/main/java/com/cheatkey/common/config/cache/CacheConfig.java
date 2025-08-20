package com.cheatkey.common.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Spring Cache 설정 - Caffeine 기반
 *
 * 탈퇴한 사용자 ID 목록을 캐싱하여 성능을 최적화합니다.
 * Caffeine은 고성능 인메모리 캐시로 JVM 힙 메모리를 효율적으로 사용합니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 탈퇴한 사용자 ID 캐시 설정
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                              // 최대 1000개 항목
                .expireAfterWrite(1, TimeUnit.HOURS)    // 1시간 후 만료
                .recordStats());                                // 통계 기록
        
        // 캐시 이름 설정
        cacheManager.setCacheNames(Collections.singleton("withdrawnUsers"));
        
        return cacheManager;
    }
}


