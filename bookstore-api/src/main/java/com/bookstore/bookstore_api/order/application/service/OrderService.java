package com.bookstore.bookstore_api.order.application.service;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.bookstore.bookstore_api.order.application.port.in.OrderUseCase;
import com.bookstore.bookstore_api.order.application.port.out.OrderItemRepository;
import com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;
import com.bookstore.bookstore_api.order.domain.model.OrderItem;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;
import com.bookstore.bookstore_api.order.domain.model.OrderLogPayload;
import com.bookstore.bookstore_api.order.domain.model.Orders;
import com.bookstore.bookstore_api.order.domain.entity.OrderResult;

import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.product.adapter.in.StockDecreaseCommand;
import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogRepository;
import com.bookstore.bookstore_api.product.domain.model.Book;
import com.bookstore.bookstore_api.order.application.event.object.OrderLogCreatedEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

        private static final Logger log = LoggerFactory.getLogger(OrderService.class);

        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final ProductRepository productRepository;
        private final OrderLogOutboxRepository orderLogOutboxRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final OrderLogRepository orderLogRepository;
        private final BlockingQueue<Long> orderLogQueue;

        @Override
        @Transactional
        public Orders createOrder(OrderCommand orderCommand) {

                try {
                        // 상품 확인
                        List<Long> productIds = orderCommand.getOrderItems().stream()
                                        .map(OrderItemCommand::getProductId)
                                        .sorted() // 현재는 인덱스가 걸린 PK로 사용하여 데드락이 발생하지 않지만, 학습 목적상 정렬 추가
                                        .toList();

                        Optional<List<Book>> products = productRepository.findAllByIdsWithLock(productIds);

                        if (!products.isPresent() || products.get().isEmpty()) {
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
                                        LocalDateTime.now(),
                                        OrderStatus.PENDING);

                        // 주문 저장
                        Orders savedOrder = orderRepository.save(newOrder);

                        if (savedOrder == null || savedOrder.getId() == null) {
                                throw new RuntimeException("주문 생성에 실패하였습니다.");
                        }

                        // OrderItem에 orderId 설정
                        List<OrderItem> orderItems = createOrderItems(orderCommand.getOrderItems())
                                        .stream()
                                        .map(item -> item.withOrderId(savedOrder.getId()))
                                        .toList();

                        // OrderItem 저장
                        orderItemRepository.saveAll(orderItems);

                        // Outbox 저장
                        OrderLogPayload orderLogPayload = OrderLogPayload.builder()
                                        .orderId(savedOrder.getId())
                                        .userId(savedOrder.getUserId())
                                        .previousStatus(null)
                                        .currentStatus(OrderStatus.PENDING)
                                        .result(OrderResult.SUCCESS)
                                        .failureReason(null)
                                        .requestId(UUID.randomUUID().toString())
                                        .occurredAt(LocalDateTime.now())
                                        .build();

                        OrderLogOutBox outbox = OrderLogOutBox.pending(
                                        savedOrder.getId(),
                                        savedOrder.getUserId(),
                                        orderLogPayload.toJson(),
                                        orderLogPayload.getRequestId());

                        OrderLogOutBox savedOutbox = orderLogOutboxRepository.save(outbox);

                        // 이벤트 발행
                        publishEventSafely(savedOutbox.getId());

                        return savedOrder;

                } catch (Exception e) {
                        // 주문 생성 실패
                        OrderLog orderLog = OrderLog.builder()
                                        .orderId(null)
                                        .userId(null)
                                        .previousStatus(null)
                                        .currentStatus(null)
                                        .result(OrderResult.FAILURE)
                                        .failureReason(e.getMessage())
                                        .requestId(null)
                                        .build();

                        orderLogRepository.save(orderLog);

                        throw new RuntimeException("주문 생성에 실패하였습니다. " + e.getMessage());
                }
        }

        /**
         * 이벤트 발행 - TaskRejectedException 발생 시 BlockingQueue에 적재
         * 이벤트 발행 실패는 주문 실패로 이어지지 않음
         */
        private void publishEventSafely(Long outboxId) {
                try {
                        eventPublisher.publishEvent(new OrderLogCreatedEvent(outboxId));
                } catch (TaskRejectedException e) {
                        log.warn("스레드 풀 포화 - OutboxId: {} BlockingQueue에 적재, Error: {}", outboxId, e.getMessage());
                        boolean queued = orderLogQueue.offer(outboxId);
                        if (!queued) {
                                log.error("BlockingQueue도 가득 참 - OutboxId: {}. processPendingOutbox 스케줄러가 처리 예정.",
                                                outboxId);
                        }
                }
        }

        /**
         * OrderItemCommand -> OrderItem 객체 변환
         *
         * @param orderItemCommands 주문 항목 명령
         * @return 주문 항목 리스트
         */
        private List<OrderItem> createOrderItems(List<OrderItemCommand> orderItemCommands) {
                return orderItemCommands.stream()
                                .map(cmd -> OrderItem.create(
                                                cmd.getProductId(),
                                                cmd.getQuantity(),
                                                cmd.getPrice()))
                                .toList();
        }
}
