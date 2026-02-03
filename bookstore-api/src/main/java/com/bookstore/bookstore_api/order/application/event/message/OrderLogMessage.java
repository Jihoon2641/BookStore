package com.bookstore.bookstore_api.order.application.event.message;

import com.bookstore.bookstore_api.order.domain.entity.OrderResult;
import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kafka 메시지용 주문 로그 DTO
 * JSON 직렬화/역직렬화를 위해 기본 생성자 필요
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogMessage {

    private Long orderId;
    private Long userId;
    private OrderStatus previousStatus;
    private OrderStatus currentStatus;
    private OrderResult result;
    private String failureReason;

    public static OrderLogMessage success(Long orderId, Long userId,
            OrderStatus previousStatus, OrderStatus currentStatus,
            OrderResult result, String failureReason) {
        return OrderLogMessage.builder()
                .orderId(orderId)
                .userId(userId)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .result(result)
                .failureReason(failureReason)
                .build();
    }

    public static OrderLogMessage failure(Long userId, String failureReason) {
        return OrderLogMessage.builder()
                .orderId(null)
                .userId(userId)
                .previousStatus(null)
                .currentStatus(OrderStatus.FAILED)
                .result(OrderResult.FAILURE)
                .failureReason(failureReason)
                .build();
    }
}
