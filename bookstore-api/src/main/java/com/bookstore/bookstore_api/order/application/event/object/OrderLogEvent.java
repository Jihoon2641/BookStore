package com.bookstore.bookstore_api.order.application.event.object;

import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;

import lombok.Getter;
import lombok.AllArgsConstructor;

import com.bookstore.bookstore_api.order.domain.entity.OrderResult;

@Getter
@AllArgsConstructor
public class OrderLogEvent {

    /**
     * 이벤트 객체
     * 
     * @param id             주문 로그 ID
     * @param orderId        주문 ID
     * @param userId         사용자 ID
     * @param previousStatus 이전 주문 상태
     * @param currentStatus  현재 주문 상태
     * @param result         주문 결과
     * @param failureReason  실패 이유
     */

    private Long id;
    private Long orderId;
    private Long userId;
    private OrderStatus previousStatus;
    private OrderStatus currentStatus;
    private OrderResult result;
    private String failureReason;

    public static OrderLogEvent success(Long orderId, Long userId,
            OrderStatus previousStatus, OrderStatus currentStatus,
            OrderResult result, String failureReason) {
        return new OrderLogEvent(
                null,
                orderId,
                userId,
                previousStatus,
                currentStatus,
                result,
                failureReason);
    }

    public static OrderLogEvent failure(Long userId, String failureReason) {
        return new OrderLogEvent(
                null,
                null,
                userId,
                null,
                OrderStatus.FAILED,
                OrderResult.FAILURE,
                failureReason);
    }
}
