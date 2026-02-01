package com.bookstore.bookstore_api.order.adapter.in;

import org.mapstruct.Mapper;

import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;

@Mapper(componentModel = "spring")
public interface OrderRequestMapper {

    /**
     * OrderRequest(DTO) → OrderCommand 변환
     * 
     * @param request
     * @return
     */
    OrderCommand toCommand(OrderRequest request);

    /**
     * OrderItemRequest(DTO) → OrderItemCommand 변환
     * 
     * @param request
     * @return
     */
    OrderItemCommand toCommand(OrderItemRequest request);

}
