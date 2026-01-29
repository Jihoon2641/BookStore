package com.bookstore.bookstore_api.order.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.adapter.in.OrderRequest;
import com.bookstore.bookstore_api.order.adapter.in.OrderRequestMapper;
import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.order.application.port.in.OrderUseCase;
import com.bookstore.bookstore_api.order.domain.model.Orders;
import com.bookstore.bookstore_api.util.response.ApiResponse;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderUseCase orderUseCase;
    private final OrderRequestMapper orderRequestMapper;

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Orders>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderCommand command = orderRequestMapper.toCommand(request);
        Orders savedOrder = orderUseCase.createOrder(command);
        return ResponseEntity.ok(ApiResponse.success(savedOrder, HttpStatus.CREATED));
    }

}
