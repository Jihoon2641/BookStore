package com.bookstore.bookstore_api.order.adapter.in;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;

@Mapper(componentModel = "spring")
public interface OrderRequestMapper {

    /**
     * OrderRequest(DTO) → OrderCommand 변환
     */
    OrderCommand toCommand(OrderRequest request);

    /**
     * OrderItemRequest(DTO) → OrderItemCommand 변환
     */
    @Mapping(target = "orderId", ignore = true)  // orderId는 주문 생성 시 null
    OrderItemCommand toCommand(OrderItemRequest request);

}
