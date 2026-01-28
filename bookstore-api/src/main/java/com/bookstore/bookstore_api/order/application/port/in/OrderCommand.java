package com.bookstore.bookstore_api.order.application.port.in;

import com.bookstore.bookstore_api.util.validate.SelfValidating;

import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

import com.bookstore.bookstore_api.order.domain.entity.OrderItemEntity;
import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;

@Getter
public class OrderCommand extends SelfValidating<OrderCommand> {

    @NotBlank(message = "주문자 ID는 필수 입력 항목입니다.")
    private final Long userId;

    @NotBlank(message = "주문 날짜는 필수 입력 항목입니다.")
    private final LocalDateTime orderDate;

    @NotBlank(message = "주문 항목은 필수 입력 항목입니다.")
    private final List<OrderItemEntity> orderItems;

    @NotBlank(message = "주문 상태는 필수 입력 항목입니다.")
    private final OrderStatus status;

    public OrderCommand(Long userId, LocalDateTime orderDate, List<OrderItemEntity> orderItems, OrderStatus status) {
        this.userId = userId;
        this.orderDate = orderDate;
        this.orderItems = orderItems;
        this.status = status;
        validateSelf();
    }
}
