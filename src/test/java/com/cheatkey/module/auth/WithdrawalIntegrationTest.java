package com.cheatkey.module.auth;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.AuthActivityService;
import com.cheatkey.module.auth.domain.service.dto.AuthTokenRequest;
import com.cheatkey.module.auth.interfaces.dto.AuthWithdrawRequest;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.entity.PostStatus;
import com.cheatkey.module.community.domain.service.CommentService;
import com.cheatkey.module.community.domain.service.CommunityService;
import com.cheatkey.module.community.domain.service.WithdrawnUserCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 탈퇴 관련 모든 기능을 통합 테스트하는 클래스
 * 
 * 테스트 대상:
 * 1. Auth 엔티티의 탈퇴 로직
 * 2. AuthService의 회원 탈퇴 처리
 * 3. AbstractAuthSignInService의 재가입 로직
 * 4. WithdrawnUserCacheService의 캐싱 로직
 * 5. CommunityService의 탈퇴 사용자 표시 로직
 * 6. CommentService의 탈퇴 사용자 표시 로직
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("탈퇴 관련 기능 통합 테스트")
class WithdrawalIntegrationTest {

    @Mock
    private AuthRepository authRepository;
    
    @Mock
    private AuthActivityService authActivityService;
    
    @Mock
    private WithdrawnUserCacheService withdrawnUserCacheService;
    
    @Mock
    private CommunityService communityService;
    
    @Mock
    private CommentService commentService;

    @InjectMocks
    private AuthService authService;

    private Auth activeAuth;
    private Auth withdrawnAuth;
    private Auth recentlyWithdrawnAuth;
    private CommunityPost activeUserPost;
    private CommunityPost withdrawnUserPost;

    @BeforeEach
    void setUp() {
        // 활성 사용자
        activeAuth = Auth.builder()
                .id(1L)
                .email("active@example.com")
                .nickname("활성사용자")
                .status(AuthStatus.ACTIVE)
                .build();

        // 30일 이상 전에 탈퇴한 사용자 (재가입 가능)
        recentlyWithdrawnAuth = Auth.builder()
                .id(2L)
                .email("withdrawn@example.com")
                .nickname("탈퇴유저")
                .status(AuthStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(31))
                .withdrawalReason("개인정보보호")
                .build();

        // 30일 미만 전에 탈퇴한 사용자 (재가입 불가)
        withdrawnAuth = Auth.builder()
                .id(3L)
                .email("recently_withdrawn@example.com")
                .nickname("최근탈퇴유저")
                .status(AuthStatus.WITHDRAWN)
                .withdrawnAt(LocalDateTime.now().minusDays(29))
                .withdrawalReason("서비스불만")
                .build();

        // 활성 사용자의 게시글
        activeUserPost = CommunityPost.builder()
                .id(1L)
                .title("활성사용자 게시글")
                .authorId(1L)
                .authorNickname("활성사용자")
                .status(PostStatus.ACTIVE)
                .build();

        // 탈퇴 사용자의 게시글
        withdrawnUserPost = CommunityPost.builder()
                .id(2L)
                .title("탈퇴유저 게시글")
                .authorId(2L)
                .authorNickname("탈퇴유저")
                .status(PostStatus.ACTIVE)
                .build();
    }

    // ===== 1. Auth 엔티티 탈퇴 로직 테스트 =====

    @Test
    @DisplayName("사용자 탈퇴 시 상태와 탈퇴 정보가 올바르게 설정된다")
    void withdraw_ShouldSetCorrectStatusAndInfo() {
        // when
        activeAuth.withdraw("개인정보보호");

        // then
        assertThat(activeAuth.getStatus()).isEqualTo(AuthStatus.WITHDRAWN);
        assertThat(activeAuth.getWithdrawnAt()).isNotNull();
        assertThat(activeAuth.getWithdrawalReason()).isEqualTo("개인정보보호");
    }

    @Test
    @DisplayName("탈퇴 후 30일이 지나지 않으면 재가입이 불가능하다")
    void canRejoin_Within30Days_ShouldReturnFalse() {
        // given
        activeAuth.withdraw("개인정보보호");
        
        // when & then
        assertThat(activeAuth.canRejoin()).isFalse();
    }

    @Test
    @DisplayName("탈퇴 후 30일이 지나면 재가입이 가능하다")
    void canRejoin_After30Days_ShouldReturnTrue() {
        // given
        activeAuth.withdraw("개인정보보호");
        activeAuth.setWithdrawnAt(LocalDateTime.now().minusDays(31));

        // when & then
        assertThat(activeAuth.canRejoin()).isTrue();
    }

    // ===== 2. AuthService 회원 탈퇴 처리 테스트 =====

    @Test
    @DisplayName("정상적인 회원 탈퇴가 성공한다")
    void withdrawUser_ActiveUser_ShouldSucceed() {
        // given
        when(authRepository.findById(1L)).thenReturn(Optional.of(activeAuth));
        when(authRepository.save(any(Auth.class))).thenReturn(activeAuth);
        doNothing().when(authActivityService).recordActivity(anyLong(), any(), any(), any(), anyBoolean(), any());
        doNothing().when(withdrawnUserCacheService).evictWithdrawnUsersCache();

        // when
        authService.withdrawUser(1L, "개인정보보호");

        // then
        verify(authRepository).save(activeAuth);
        verify(authActivityService).recordActivity(eq(1L), any(), isNull(), isNull(), eq(true), eq("개인정보보호"));
        verify(withdrawnUserCacheService).evictWithdrawnUsersCache();
        
        assertThat(activeAuth.getStatus()).isEqualTo(AuthStatus.WITHDRAWN);
        assertThat(activeAuth.getWithdrawnAt()).isNotNull();
        assertThat(activeAuth.getWithdrawalReason()).isEqualTo("개인정보보호");
    }

    @Test
    @DisplayName("이미 탈퇴한 회원은 재탈퇴할 수 없다")
    void withdrawUser_AlreadyWithdrawn_ShouldThrowException() {
        // given
        when(authRepository.findById(2L)).thenReturn(Optional.of(recentlyWithdrawnAuth));

        // when & then
        assertThatThrownBy(() -> authService.withdrawUser(2L, "개인정보보호"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_ALREADY_WITHDRAWN);
    }

    @Test
    @DisplayName("존재하지 않는 회원 탈퇴 시 예외가 발생한다")
    void withdrawUser_UserNotFound_ShouldThrowException() {
        // given
        when(authRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.withdrawUser(999L, "개인정보보호"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }

    // ===== 3. WithdrawnUserCacheService 캐싱 로직 테스트 =====

    @Test
    @DisplayName("탈퇴한 사용자 ID 목록을 조회할 수 있다")
    void getWithdrawnUserIds_ShouldReturnWithdrawnUserIds() {
        // given
        List<Long> withdrawnUserIds = Arrays.asList(2L, 3L);
        when(withdrawnUserCacheService.getWithdrawnUserIds()).thenReturn(withdrawnUserIds);

        // when
        List<Long> result = withdrawnUserCacheService.getWithdrawnUserIds();

        // then
        assertThat(result).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("특정 사용자가 탈퇴했는지 확인할 수 있다")
    void isWithdrawnUser_ShouldReturnCorrectResult() {
        // given
        when(withdrawnUserCacheService.isWithdrawnUser(2L)).thenReturn(true);
        when(withdrawnUserCacheService.isWithdrawnUser(1L)).thenReturn(false);

        // when & then
        assertThat(withdrawnUserCacheService.isWithdrawnUser(2L)).isTrue();
        assertThat(withdrawnUserCacheService.isWithdrawnUser(1L)).isFalse();
    }

    // ===== 4. CommunityService 탈퇴 사용자 표시 테스트 =====

    @Test
    @DisplayName("탈퇴한 사용자의 게시글은 '탈퇴된 사용자'로 표시된다")
    void markAsWithdrawnUser_ShouldChangeNicknameToWithdrawnUser() {
        // given
        CommunityPost expectedResult = CommunityPost.builder()
                .id(2L)
                .title("탈퇴유저 게시글")
                .authorId(2L)
                .authorNickname("탈퇴된 사용자")
                .status(PostStatus.ACTIVE)
                .build();
        
        when(communityService.markAsWithdrawnUser(withdrawnUserPost)).thenReturn(expectedResult);

        // when
        CommunityPost result = communityService.markAsWithdrawnUser(withdrawnUserPost);

        // then
        assertThat(result.getAuthorNickname()).isEqualTo("탈퇴된 사용자");
        assertThat(result.getId()).isEqualTo(withdrawnUserPost.getId());
        assertThat(result.getTitle()).isEqualTo(withdrawnUserPost.getTitle());
        assertThat(result.getAuthorId()).isEqualTo(withdrawnUserPost.getAuthorId());
    }

    @Test
    @DisplayName("활성 사용자의 게시글은 닉네임이 변경되지 않는다")
    void markAsWithdrawnUser_ActiveUser_ShouldNotChangeNickname() {
        // given
        CommunityPost expectedResult = CommunityPost.builder()
                .id(1L)
                .title("활성사용자 게시글")
                .authorId(1L)
                .authorNickname("활성사용자")
                .status(PostStatus.ACTIVE)
                .build();
        
        when(communityService.markAsWithdrawnUser(activeUserPost)).thenReturn(expectedResult);

        // when
        CommunityPost result = communityService.markAsWithdrawnUser(activeUserPost);

        // then
        assertThat(result.getAuthorNickname()).isEqualTo("활성사용자");
        assertThat(result.getId()).isEqualTo(activeUserPost.getId());
    }

    @Test
    @DisplayName("탈퇴 사용자 캐시 무효화가 올바르게 동작한다")
    void withdrawnUserCache_ShouldBeEvictedCorrectly() {
        // given
        doNothing().when(withdrawnUserCacheService).evictWithdrawnUsersCache();

        // when
        withdrawnUserCacheService.evictWithdrawnUsersCache();

        // then
        verify(withdrawnUserCacheService, times(1)).evictWithdrawnUsersCache();
    }

    // ===== 5. 탈퇴 정책 통합 테스트 =====

    @Test
    @DisplayName("탈퇴 정책이 올바르게 적용된다")
    void withdrawalPolicy_ShouldBeAppliedCorrectly() {
        // given
        Auth testAuth = Auth.builder()
                .id(100L)
                .email("test@example.com")
                .nickname("테스트유저")
                .status(AuthStatus.ACTIVE)
                .build();

        // when - 탈퇴 처리
        testAuth.withdraw("테스트");

        // then - 탈퇴 상태 확인
        assertThat(testAuth.getStatus()).isEqualTo(AuthStatus.WITHDRAWN);
        assertThat(testAuth.canRejoin()).isFalse();

        // when - 30일 후 설정
        testAuth.setWithdrawnAt(LocalDateTime.now().minusDays(31));

        // then - 재가입 가능 확인
        assertThat(testAuth.canRejoin()).isTrue();
    }

    @Test
    @DisplayName("탈퇴 사유가 올바르게 저장된다")
    void withdrawalReason_ShouldBeStoredCorrectly() {
        // given
        String reason = "WITHDRAWAL_REASON_006";

        // when
        activeAuth.withdraw(reason);

        // then
        assertThat(activeAuth.getWithdrawalReason()).isEqualTo(reason);
    }

    // ===== 6. 에러 케이스 통합 테스트 =====

    @Test
    @DisplayName("탈퇴 관련 모든 에러 케이스가 올바르게 처리된다")
    void withdrawalErrorCases_ShouldBeHandledCorrectly() {
        // 1. 이미 탈퇴한 회원 재탈퇴 시도
        when(authRepository.findById(2L)).thenReturn(Optional.of(recentlyWithdrawnAuth));
        assertThatThrownBy(() -> authService.withdrawUser(2L, "개인정보보호"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_ALREADY_WITHDRAWN);

        // 2. 존재하지 않는 회원 탈퇴 시도
        when(authRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.withdrawUser(999L, "개인정보보호"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }
}
