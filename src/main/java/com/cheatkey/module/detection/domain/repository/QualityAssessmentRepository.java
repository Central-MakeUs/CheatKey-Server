package com.cheatkey.module.detection.domain.repository;

import com.cheatkey.module.detection.domain.entity.QualityAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityAssessmentRepository extends JpaRepository<QualityAssessment, Long> {

    // ===== 현재 구현 (기본 CRUD) =====
    // save(), findById(), findAll(), delete() 등 기본 메서드 제공

    /**
     * 📊 품질 평가 데이터 분석 활용 방안
     *
     * 향후 고도화 시 다음과 같은 분석이 가능합니다:
     *
     * 1. 품질 점수 트렌드 분석
     *    - 시간별 품질 변화 추이
     *    - AI 모델 성능 개선 효과 측정
     *
     * 2. 액션 타입별 분포 분석
     *    - 즉시 조치가 필요한 케이스 비율
     *    - 수동 검토가 필요한 케이스 비율
     *
     * 3. 품질과 유사도 점수의 상관관계
     *    - 높은 유사도 ≠ 높은 품질인 경우 분석
     *    - 품질 평가 모델 개선 방향 제시
     *
     * 4. 사용자별 품질 차이 분석
     *    - 개인별 입력 품질 패턴
     *    - 사용자 교육이 필요한 영역 파악
     */

    /**
     * 🔍 성능 최적화 고려사항
     *
     * 품질 평가 데이터가 대량으로 쌓일 경우를 대비한 최적화:
     *
     * 1. 인덱스 최적화
     *    - assessmentTime, overallScore, actionType 등 자주 조회되는 필드
     *    - 복합 인덱스로 조회 성능 향상
     *
     * 2. 파티셔닝 고려
     *    - 시간별 파티셔닝으로 오래된 데이터 관리
     *    - 월별/연도별 파티션으로 조회 성능 향상
     *
     * 3. 아카이빙 전략
     *    - 일정 기간이 지난 데이터는 별도 테이블로 이동
     *    - 분석용 데이터와 운영용 데이터 분리
     */
}