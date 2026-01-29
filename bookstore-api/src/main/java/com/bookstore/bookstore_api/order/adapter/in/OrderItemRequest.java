package com.bookstore.bookstore_api.order.adapter.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
    @NotNull(message = "상품 ID는 필수 입력 항목입니다.")
    Long productId,

    @NotNull(message = "가격은 필수 입력 항목입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    Long price,

    @NotNull(message = "수량은 필수 입력 항목입니다.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    Long quantity
) {}
