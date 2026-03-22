package com.bookstore.bookstore_api.order.application.port.out;

import java.util.List;

import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;

public interface OrderLogOutboxRepository {

    /**
     * Outbox 레코드 저장
     * 
     * @param orderLogOutBox 도메인 모델
     * @return 저장된 행 수
     */
    OrderLogOutBox save(OrderLogOutBox orderLogOutBox);

    /**
     * PENDING 상태의 Outbox 레코드 조회
     * 
     * @param limit 최대 조회 개수
     * @return PENDING 상태의 Outbox 목록
     */
    List<OrderLogOutBox> findPendingWithLimit(int limit);

    /**
     * 상태를 SENT로 업데이트
     * 
     * @param id Outbox ID
     * @return 업데이트된 행 수
     */
    int markAsSent(Long id);

    /**
     * 실패 처리 (retry_count 증가, last_error 기록)
     * 
     * @param id        Outbox ID
     * @param lastError 에러 메시지
     * @param maxRetry  최대 재시도 횟수
     * @return 업데이트된 행 수
     */
    int markAsFailed(Long id, String lastError, int maxRetry);

    /**
     * FAILED 상태의 Outbox 레코드 조회
     * 
     * @param limit 최대 조회 개수
     * @return FAILED 상태의 Outbox 목록
     */
    List<OrderLogOutBox> findFailedWithLimit(int limit);

    /**
     * ID로 Outbox 조회
     * 
     * @param id Outbox ID
     * @return OrderLogOutBox
     */
    OrderLogOutBox findById(Long id);

    /**
     * Outbox 업데이트
     * 
     * @param orderLogOutBox
     * @return OrderLogOutBox
     */
    OrderLogOutBox update(OrderLogOutBox orderLogOutBox);

    /**
     * FAILED 상태를 PENDING으로 되돌림 (Debezium이 읽을 수 있도록)
     * 
     * @param id
     * @return
     */
    int resetFailedToPending(Long id);
}