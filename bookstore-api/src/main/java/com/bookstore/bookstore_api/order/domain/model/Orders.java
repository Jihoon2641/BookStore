package com.bookstore.bookstore_api.order.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;

@Schema(description = "주문 정보")
@Getter
@AllArgsConstructor
public class Orders {

    @Schema(description = "주문 ID")
    private Long id;
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "주문 날짜")
    private LocalDateTime orderDate;
    @Schema(description = "주문 상태")
    private OrderStatus status;

    /**
     * 신규 주문 생성
     * @param userId 사용자 ID
     * @param orderDate 주문 날짜
     * @param status 주문 상태
     * @return 신규 주문 정보
     */
    public static Orders create(Long userId, LocalDateTime orderDate, OrderStatus status) {
        validateUserId(userId);
        return new Orders(null, userId, orderDate, status);
    }

    /* =============== 검증 메서드 =============== */

    /**
     * 사용자 ID 유효성 검사
     * @param userId 사용자 ID
     */
    private static void validateUserId(Long userId) {
        if (userId == null || userId < 0) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

}
