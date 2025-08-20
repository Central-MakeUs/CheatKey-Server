package com.cheatkey.module.detection.domain.repository;

import com.cheatkey.module.detection.domain.entity.DetectionWorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionWorkflowStateRepository extends JpaRepository<DetectionWorkflowState, Long> {

    // ===== 현재 구현 (기본 CRUD) =====
    // save(), findById(), findAll(), delete() 등 기본 메서드 제공

    // ===== 낙관적 락 관련 주석 =====

    /**
     * 🚨 중요: 낙관적 락 구현 필요
     *
     * 현재 DetectionWorkflowState는 워크플로우 진행 중 계속 업데이트되므로
     * 동시성 문제가 발생할 수 있습니다.
     *
     * 해결 방안:
     * 1. DetectionWorkflowState 엔티티에 @Version 필드 추가
     * 2. 저장 시 ObjectOptimisticLockingFailureException 처리
     * 3. 충돌 시 적절한 재시도 로직 구현
     *
     * 예시 코드:
     * @Entity
     * public class DetectionWorkflowState {
     *     @Version
     *     private Long version; // 낙관적 락용
     * }
     *
     * 서비스 레이어에서:
     * try {
     *     return repository.save(workflowState);
     * } catch (ObjectOptimisticLockingFailureException e) {
     *     // 재시도 로직 또는 적절한 에러 처리
     * }
     */

    /**
     * 🚨 주의: 트랜잭션 범위 최적화 필요
     *
     * 현재 워크플로우 실행이 트랜잭션 내에서 이루어지므로
     * 긴 실행 시간으로 인한 동시성 저하가 발생할 수 있습니다.
     *
     * 해결 방안:
     * 1. 워크플로우 실행과 DB 저장을 분리
     * 2. 워크플로우 실행은 트랜잭션 외부에서 처리
     * 3. 결과 저장만 트랜잭션으로 처리
     *
     * 예시 구조:
     * public DetectionResponse detect(...) {
     *     // 1. 트랜잭션 없이 워크플로우 실행
     *     DetectionWorkflowState state = workflow.executeWorkflow(...);
     *
     *     // 2. 결과만 트랜잭션으로 저장
     *     return saveDetectionResult(state, ...);
     * }
     *
     * @Transactional
     * private DetectionResponse saveDetectionResult(...) {
     *     // DB 저장 로직
     * }
     */
}