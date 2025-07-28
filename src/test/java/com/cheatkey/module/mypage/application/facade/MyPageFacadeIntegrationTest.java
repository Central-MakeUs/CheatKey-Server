package com.cheatkey.module.mypage.application.facade;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.ProfileImage;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.repository.ProfileImageRepository;
import com.cheatkey.module.community.domian.entity.CommunityCategory;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.entity.PostStatus;
import com.cheatkey.module.community.domian.repository.CommunityPostRepository;
import com.cheatkey.module.detection.domain.entity.DetectionHistory;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.mypage.application.facade.MyPageFacade;
import com.cheatkey.module.mypage.interfaces.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MyPageFacadeIntegrationTest {

    @Autowired
    private MyPageFacade myPageFacade;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private ProfileImageRepository profileImageRepository;

    @Autowired
    private CommunityPostRepository communityPostRepository;

    @Autowired
    private DetectionHistoryRepository detectionHistoryRepository;

    private static final Long TEST_USER_ID = 1L;
    private Auth testUser;
    private ProfileImage testProfileImage;

    @BeforeEach
    void setUp() {
        // 기존 테스트 사용자 조회 (이미 DB에 존재)
        testUser = authRepository.findById(TEST_USER_ID)
                .orElseThrow(() -> new RuntimeException("테스트 사용자(ID=1)가 존재하지 않습니다."));

        // 기존 프로필 이미지 조회 (이미 DB에 존재)
        testProfileImage = profileImageRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("테스트 프로필 이미지(ID=1)가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("마이페이지 대시보드 조회 성공")
    void 마이페이지_대시보드_조회_성공() {
        // when
        MyPageDashboardResponse response = myPageFacade.getMyPageDashboard(TEST_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserInfo()).isNotNull();
        assertThat(response.getUserInfo().getNickname()).isNotNull(); // 실제 DB 데이터에 따라 검증
        assertThat(response.getUserInfo().getTotalVisitCount()).isNotNull(); // 실제 DB 데이터에 따라 검증
        assertThat(response.getProfileImages()).isNotEmpty();
    }

    @Test
    @DisplayName("사용자 작성글 목록 조회 성공")
    void 사용자_작성글_목록_조회_성공() {
        // given - 테스트용 게시글 생성
        CommunityPost post = CommunityPost.builder()
                .userId(TEST_USER_ID)
                .nickname("테스터")
                .title("테스트 게시글")
                .content("테스트 내용")
                .status(PostStatus.ACTIVE)
                .category(CommunityCategory.REPORT)
                .viewCount(5L)
                .createdAt(LocalDateTime.now())
                .build();
        communityPostRepository.save(post);

        // when
        Pageable pageable = PageRequest.of(0, 20);
        UserPostManagementResponse response = myPageFacade.getUserPostManagement(TEST_USER_ID, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPosts()).hasSize(1);
        assertThat(response.getPosts().get(0).getTitle()).isEqualTo("테스트 게시글");
        assertThat(response.getPosts().get(0).getNickname()).isEqualTo("테스터");
        assertThat(response.getTotalPosts()).isEqualTo(1);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("분석 내역 조회 성공")
    void 분석_내역_조회_성공() {
        // given - 테스트용 분석 내역 생성
        DetectionHistory history = DetectionHistory.builder()
                .userId(TEST_USER_ID)
                .status(DetectionStatus.SAFE)
                .detectionType("URL")
                .inputText("https://example.com")
                .detectedAt(LocalDateTime.now())
                .build();
        detectionHistoryRepository.save(history);

        // when
        Pageable pageable = PageRequest.of(0, 20);
        Page<DetectionHistoryResponse> response = myPageFacade.getDetectionHistory(TEST_USER_ID, "all", pageable);

        // then
        // 기존 데이터가 있을 수 있으므로 최소 1개 이상으로 검증
        assertThat(response.getContent()).hasSizeGreaterThanOrEqualTo(1);
        // 새로 생성한 데이터가 포함되어 있는지 확인
        boolean hasNewData = response.getContent().stream()
                .anyMatch(r -> r.getInputText().equals("https://example.com"));
        assertThat(hasNewData).isTrue();
    }

    @Test
    @DisplayName("기간별 분석 내역 조회 성공")
    void 기간별_분석_내역_조회_성공() {
        // given - 테스트용 분석 내역 생성 (고유한 텍스트로 구분)
        String uniqueTodayText = "https://today-test-" + System.currentTimeMillis() + ".com";
        String uniqueOldText = "https://old-test-" + System.currentTimeMillis() + ".com";
        
        DetectionHistory todayHistory = DetectionHistory.builder()
                .userId(TEST_USER_ID)
                .status(DetectionStatus.SAFE)
                .detectionType("URL")
                .inputText(uniqueTodayText)
                .detectedAt(LocalDateTime.now())
                .build();
        detectionHistoryRepository.save(todayHistory);

        DetectionHistory oldHistory = DetectionHistory.builder()
                .userId(TEST_USER_ID)
                .status(DetectionStatus.DANGER)
                .detectionType("CASE")
                .inputText(uniqueOldText)
                .detectedAt(LocalDateTime.now().minusDays(10))
                .build();
        DetectionHistory savedOldHistory = detectionHistoryRepository.save(oldHistory);

        // when
        Pageable pageable = PageRequest.of(0, 20);
        Page<DetectionHistoryResponse> todayResponse = myPageFacade.getDetectionHistory(TEST_USER_ID, "today", pageable);
        Page<DetectionHistoryResponse> allResponse = myPageFacade.getDetectionHistory(TEST_USER_ID, "all", pageable);

        // then
        // todayResponse에는 오늘 생성한 데이터가 포함되어 있어야 함
        boolean hasTodayData = todayResponse.getContent().stream()
                .anyMatch(r -> r.getInputText().equals(uniqueTodayText));
        assertThat(hasTodayData).isTrue();
        
        // allResponse에는 두 데이터 모두 포함되어 있어야 함
        boolean hasTodayDataInAll = allResponse.getContent().stream()
                .anyMatch(r -> r.getInputText().equals(uniqueTodayText));
        boolean hasOldDataInAll = allResponse.getContent().stream()
                .anyMatch(r -> r.getInputText().equals(uniqueOldText));
        assertThat(hasTodayDataInAll).isTrue();
        assertThat(hasOldDataInAll).isTrue();
    }

    @Test
    @DisplayName("빈 작성글 목록 조회 성공")
    void 빈_작성글_목록_조회_성공() {
        // when
        Pageable pageable = PageRequest.of(0, 20);
        UserPostManagementResponse response = myPageFacade.getUserPostManagement(TEST_USER_ID, pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPosts()).isEmpty();
        assertThat(response.getTotalPosts()).isEqualTo(0);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getPageSize()).isEqualTo(20);
    }
} 