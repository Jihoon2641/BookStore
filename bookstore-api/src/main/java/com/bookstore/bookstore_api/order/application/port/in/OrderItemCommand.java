package com.bookstore.bookstore_api.order.application.port.in;

import lombok.Getter;
import com.bookstore.bookstore_api.util.validate.SelfValidating;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Min;

@Getter
public class OrderItemCommand extends SelfValidating<OrderItemCommand> {

    @NotBlank(message = "주문 ID는 필수 입력 항목입니다.")
    private final Long orderId;

    @NotBlank(message = "상품 ID는 필수 입력 항목입니다.")
    private final Long productId;

    @NotBlank(message = "가격은 필수 입력 항목입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private final Long price;

    @NotBlank(message = "수량은 필수 입력 항목입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private final Long quantity;

    public OrderItemCommand(Long orderId, Long productId, Long price, Long quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;  
        validateSelf();
    }
}
