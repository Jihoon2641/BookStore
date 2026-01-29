package com.bookstore.bookstore_api.order.application.port.in;

import com.bookstore.bookstore_api.util.validate.SelfValidating;

import lombok.Getter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class OrderCommand extends SelfValidating<OrderCommand> {

    @NotNull(message = "주문자 ID는 필수 입력 항목입니다.")
    private final Long userId;

    @NotNull(message = "주문 날짜는 필수 입력 항목입니다.")
    private final LocalDateTime orderDate;

    @NotEmpty(message = "주문 항목은 필수 입력 항목입니다.")
    private final List<OrderItemCommand> orderItems;

    public OrderCommand(Long userId, LocalDateTime orderDate, List<OrderItemCommand> orderItems) {
        this.userId = userId;
        this.orderDate = orderDate;
        this.orderItems = orderItems;
        validateSelf();
    }
}
