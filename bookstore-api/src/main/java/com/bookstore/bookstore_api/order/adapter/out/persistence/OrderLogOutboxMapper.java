package com.bookstore.bookstore_api.order.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookstore.bookstore_api.order.domain.entity.OrderLogOutboxEntity;

@Mapper
public interface OrderLogOutboxMapper {

    int save(OrderLogOutboxEntity orderLogOutboxEntity);

    /**
     * PENDING 상태의 Outbox 레코드 조회
     * 
     * @param limit 최대 조회 개수
     * @return PENDING 상태의 Outbox 목록
     */
    List<OrderLogOutboxEntity> findPendingWithLimit(@Param("limit") int limit);

    /**
     * 상태를 SENT로 업데이트
     * 
     * @param id     Outbox ID
     * @param sentAt 전송 시각
     * @return 업데이트된 행 수
     */
    int updateStatusToSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    /**
     * 실패 시 retry_count 증가 및 last_error 업데이트
     * 
     * @param id        Outbox ID
     * @param lastError 에러 메시지
     * @param maxRetry  최대 재시도 횟수 (초과 시 FAILED로 변경)
     * @return 업데이트된 행 수
     */
    int updateRetryAndError(@Param("id") Long id, @Param("lastError") String lastError, @Param("maxRetry") int maxRetry);
}
