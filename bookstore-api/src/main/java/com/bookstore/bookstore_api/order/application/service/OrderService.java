package com.bookstore.bookstore_api.order.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.application.port.in.OrderUseCase;
import com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderItem;
import com.bookstore.bookstore_api.order.domain.model.Orders;

import jakarta.transaction.Transactional;

import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;
import com.bookstore.bookstore_api.product.domain.model.Book;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Orders createOrder(OrderCommand orderCommand) {

        // 1. 로그 저장
        
        // 2. 정상적인 상품인지 확인 - 한꺼번에 보내기 현재는 따로 보내는 중이므로 수정 필요
        for (OrderItemCommand orderItemCommand : orderCommand.getOrderItems()) {
            Optional<Book> product = productRepository.findById(orderItemCommand.getProductId());
            if (product.isEmpty()) {
                throw new RuntimeException("상품이 존재하지 않습니다.");
            }
        }
        // 3. 수량 확인
        // 3. 재고 차감
        
        // 4. 주문 생성
        Orders newOrder = Orders.create(
            orderCommand.getUserId(), 
            orderCommand.getOrderDate(), 
            createOrderItems(orderCommand.getOrderItems()),
            orderCommand.getStatus()
        );

        // 5. 주문 저장
        Orders savedOrder = orderRepository.save(newOrder);

        if (savedOrder == null) {
            throw new RuntimeException("주문 생성에 실패하였습니다.");
        }

        // 6. 로그 저장

        return savedOrder;
    }

    /**
     * 주문 항목 생성
     * @param orderItemCommands 주문 항목 명령
     * @return 주문 항목 리스트
     */
    private List<OrderItem> createOrderItems(List<OrderItemCommand> orderItemCommands) {
        return orderItemCommands.stream()
            .map(cmd -> OrderItem.create(
                cmd.getOrderId(), 
                cmd.getProductId(), 
                cmd.getQuantity(), 
                cmd.getPrice()
            ))
            .toList();
    }
}
