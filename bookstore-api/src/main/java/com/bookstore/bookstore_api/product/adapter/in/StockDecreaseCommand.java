package com.bookstore.bookstore_api.product.adapter.in;

import com.bookstore.bookstore_api.util.validate.SelfValidating;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StockDecreaseCommand extends SelfValidating<StockDecreaseCommand> {

    @NotNull(message = "상품 ID는 필수 입력 항목입니다.")
    private final Long productId;

    @NotNull(message = "수량은 필수 입력 항목입니다.")
    private final Long quantity;

    public StockDecreaseCommand(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.validateSelf();
    }

}
