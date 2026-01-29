package com.bookstore.bookstore_api.order.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.application.port.in.OrderUseCase;
import com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderItem;
import com.bookstore.bookstore_api.order.domain.model.Orders;

import jakarta.transaction.Transactional;

import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.product.adapter.in.BookCommand;
import com.bookstore.bookstore_api.product.adapter.in.StockDecreaseCommand;
import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;
import com.bookstore.bookstore_api.product.domain.model.Book;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    // TODO: 추후 Global Exception Handler 적용 필요
    public Orders createOrder(OrderCommand orderCommand) {
        
        // 상품 확인
        List<Long> productIds = orderCommand.getOrderItems().stream()
            .map(OrderItemCommand::getProductId)
            .toList();

        Optional<List<Book>> products = productRepository.findAllByIds(productIds);

        if (products.isPresent() && !products.get().isEmpty()) {
            throw new RuntimeException("상품이 존재하지 않습니다.");
        }

        // 수량 확인 및 비교
        Map<Long, Book> productMap = new HashMap<>();
        for (OrderItemCommand orderItemCommand : orderCommand.getOrderItems()) {
            productMap = products.get().stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

            Book product = productMap.get(orderItemCommand.getProductId());

            if (product.getStock() < orderItemCommand.getQuantity()) {
                throw new RuntimeException("상품 수량이 부족합니다.");
            }
        }

        // 재고 차감
        List<StockDecreaseCommand> stockDecreaseCommands = orderCommand.getOrderItems().stream()
            .map(cmd -> new StockDecreaseCommand(cmd.getProductId(), cmd.getQuantity()))
            .toList();

        productRepository.updateStock(stockDecreaseCommands);

        // 주문 생성
        Orders newOrder = Orders.create(
            orderCommand.getUserId(), 
            orderCommand.getOrderDate(), 
            createOrderItems(orderCommand.getOrderItems()),
            orderCommand.getStatus()
        );

        // 주문 저장
        Orders savedOrder = orderRepository.save(newOrder);

        if (savedOrder == null) {
            throw new RuntimeException("주문 생성에 실패하였습니다.");
        }

        // 로그 저장

        return savedOrder;
    }

    /**
     * orderItem -> OrderItem 객체 변환
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
