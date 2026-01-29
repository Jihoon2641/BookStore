package com.bookstore.bookstore_api.order.adapter.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotNull(message = "주문자 ID는 필수 입력 항목입니다.") Long userId,

        @NotEmpty(message = "주문 항목은 필수 입력 항목입니다.") @Valid List<OrderItemRequest> orderItems) {
}
