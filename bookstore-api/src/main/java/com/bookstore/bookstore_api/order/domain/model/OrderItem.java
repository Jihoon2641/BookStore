package com.bookstore.bookstore_api.order.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "주문 항목 정보")
@Getter
@AllArgsConstructor
public class OrderItem {

    @Schema(description = "주문 항목 ID")
    private Long id;
    @Schema(description = "주문 ID")
    private Long orderId;
    @Schema(description = "도서 ID")
    private Long bookId;
    @Schema(description = "주문 수량")
    private Long quantity;
    @Schema(description = "주문 가격")
    private Long price;

    /**
     * 신규 주문 항목 생성 (주문 생성 시 orderId 없이)
     * @param bookId 도서 ID
     * @param quantity 주문 수량
     * @param price 주문 가격
     * @return 신규 주문 항목 정보
     */
    public static OrderItem create(Long bookId, Long quantity, Long price) {
        validateBookId(bookId);
        validateQuantity(quantity);
        validatePrice(price);

        return new OrderItem(null, null, bookId, quantity, price);
    }

    /**
     * orderId를 설정한 새 OrderItem 반환
     * @param orderId 주문 ID
     * @return orderId가 설정된 OrderItem
     */
    public OrderItem withOrderId(Long orderId) {
        return new OrderItem(this.id, orderId, this.bookId, this.quantity, this.price);
    }

    /* =============== 검증 메서드 =============== */

    /**
     * 도서 ID 유효성 검사
     * @param bookId 도서 ID
     */
    private static void validateBookId(Long bookId) {
        if (bookId == null || bookId < 0) {
            throw new IllegalArgumentException("도서 ID는 필수입니다.");
        }
    }

    /**
     * 주문 수량 유효성 검사
     * @param quantity 주문 수량
     */
    private static void validateQuantity(Long quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("주문 수량은 필수이며, 1 이상이어야 합니다.");
        }
    }

    /**
     * 주문 가격 유효성 검사
     * @param price 주문 가격
     */
    private static void validatePrice(Long price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("주문 가격은 필수이며, 0원 이상이어야 합니다.");
        }
    }
}
