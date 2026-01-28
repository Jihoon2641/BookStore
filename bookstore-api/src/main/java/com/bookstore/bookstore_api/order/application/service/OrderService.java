package com.bookstore.bookstore_api.order.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.application.port.in.OrderUseCase;
import com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
import com.bookstore.bookstore_api.order.domain.model.Orders;

import jakarta.transaction.Transactional;

import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Orders createOrder(OrderCommand orderCommand) {

        // 1. 로그 저장
        // 2. 정상적인 상품인지 확인
        // 3. 수량 확인
        // 4. 재고 차감
        
        // 5. 주문 생성
        Orders newOrder = Orders.create(
            orderCommand.getUserId(), 
            orderCommand.getOrderDate(), 
            orderCommand.getOrderItems(), 
            orderCommand.getStatus()
        );

        // 6. 주문 저장
        Orders savedOrder = orderRepository.save(newOrder);

        if (savedOrder == null) {
            throw new RuntimeException("주문 생성에 실패하였습니다.");
        }

        // 7. 로그 저장

        return savedOrder;
    }
}
