package com.bookstore.bookstore_api.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;
import com.bookstore.bookstore_api.order.domain.entity.OrderResult;

@Schema(description = "주문 로그 정보")
@Getter
@AllArgsConstructor
@Builder
public class OrderLog {

    @Schema(description = "주문 로그 ID")
    private Long id;
    @Schema(description = "주문 ID")
    private Long orderId;
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "이전 주문 상태")
    private OrderStatus previousStatus;
    @Schema(description = "현재 주문 상태")
    private OrderStatus currentStatus;
    @Schema(description = "주문 결과")
    private OrderResult result;
    @Schema(description = "실패 이유")
    private String failureReason;
    @Schema(description = "요청 ID")
    private String requestId;

    /**
     * 신규 주문 로그 생성
     * 
     * @param orderId        주문 ID
     * @param userId         사용자 ID
     * @param previousStatus 이전 주문 상태
     * @param currentStatus  현재 주문 상태
     * @param result         주문 결과
     * @param failureReason  실패 이유
     * @param description    설명
     * @return 신규 주문 로그 정보
     */
    public static OrderLog create(Long orderId, Long userId, OrderStatus previousStatus, OrderStatus currentStatus,
            OrderResult result, String failureReason, String requestId) {
        validateOrderId(orderId);
        validateUserId(userId);
        return new OrderLog(null, orderId, userId, previousStatus, currentStatus, result, failureReason, requestId);
    }

    /* =============== 검증 메서드 =============== */

    /**
     * 주문 ID 유효성 검사
     * 
     * @param orderId 주문 ID
     */
    private static void validateOrderId(Long orderId) {
        if (orderId == null || orderId < 0) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
    }

    /**
     * 사용자 ID 유효성 검사
     * 
     * @param userId 사용자 ID
     */
    private static void validateUserId(Long userId) {
        if (userId == null || userId < 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

}
