package com.bookstore.bookstore_api.order.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.bookstore.bookstore_api.order.application.port.in.OrderUseCase;
import com.bookstore.bookstore_api.order.application.port.out.OrderItemRepository;
import com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;
import com.bookstore.bookstore_api.order.domain.model.OrderItem;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;
import com.bookstore.bookstore_api.order.domain.model.Orders;
import com.bookstore.bookstore_api.order.domain.entity.OrderResult;

import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.order.application.event.object.OrderLogEvent;
import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
import com.bookstore.bookstore_api.product.adapter.in.StockDecreaseCommand;
import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;
import com.bookstore.bookstore_api.product.domain.model.Book;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Primary
@SuppressWarnings("null")
@Slf4j
public class OrderDeadlockService implements OrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Orders createOrder(OrderCommand orderCommand) {

        try {
            // 유저 확인 (Security?)

            // 1. 상품 IDs 추출 (데드락 유도를 위해 정렬하지 않음)
            List<Long> productIds = orderCommand.getOrderItems().stream()
                    .map(OrderItemCommand::getProductId)
                    .toList();

            // 3. 상품 조회 (비관적 락 - PK 기준)
            // 인덱스가 있는 PK로 조회하여 정확한 행 단위 락을 유도
            // 하나씩 조회하여 데드락 유도 확률을 높임
            log.info("[Deadlock-Test] Acquiring Individual Locks for product IDs: {}", productIds);

            List<Book> productsList = new ArrayList<>();
            for (Long productId : productIds) {
                log.info("[Deadlock-Test] Acquiring Lock for product ID: {}", productId);

                // 데드락 유도를 위한 인위적 지연 (각 락 획득 사이)
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Lock acquisition interrupted", ie);
                }

                Optional<Book> bookOpt = productRepository.findByIdWithLock(productId);
                if (bookOpt.isPresent()) {
                    productsList.add(bookOpt.get());
                    log.info("[Deadlock-Test] Lock Acquired for product ID: {}", productId);
                } else {
                    log.warn("[Deadlock-Test] Book not found for product ID: {}", productId);
                }
            }

            if (productsList.isEmpty()) {
                throw new RuntimeException("상품이 존재하지 않습니다. (Lock Acquire Failed or Data Missing)");
            }

            Optional<List<Book>> products = Optional.of(productsList);

            log.info("[Deadlock-Test] All individual locks acquired for product IDs: {}", productIds);

            if (!products.isPresent() || products.get().isEmpty()) {
                throw new RuntimeException("상품이 존재하지 않습니다. (Lock Acquire Failed or Data Missing)");
            }

            // 수량 확인 및 비교
            Map<Long, Book> productMap = new HashMap<>();
            for (OrderItemCommand orderItemCommand : orderCommand.getOrderItems()) {
                // 주의: 여기서 productMap은 Lock으로 가져온 데이터를 기반으로 해야 함
                // Title로 가져왔어도 ID는 매핑되어야 함.
                // 만약 Title 중복이 있다면? (현재 로직상 Title 중복은 고려하지 않음. BookEntity title은 unique가 아닐 수도
                // 있지만, 여기선 테스트 목적)
                productMap = products.get().stream()
                        .collect(Collectors.toMap(Book::getId, Function.identity(), (p1, p2) -> p1)); // 중복 시 첫 번째 것 사용

                Book product = productMap.get(orderItemCommand.getProductId());

                if (product == null) {
                    // ID로 조회했을 땐 있었으나 Title로 다시 조회했을 때 매칭이 안될 가능성 희박하지만 체크
                    throw new RuntimeException("상품 정보를 찾을 수 없습니다.");
                }

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

            // 로그 저장 -> selfValidate 검증
            OrderLog orderLog = OrderLog.create(
                    savedOrder.getId(),
                    savedOrder.getUserId(),
                    null,
                    OrderStatus.PENDING,
                    OrderResult.SUCCESS,
                    null);

            // 이벤트 객체 생성
            OrderLogEvent orderLogEvent = OrderLogEvent.success(
                    orderLog.getOrderId(),
                    orderLog.getUserId(),
                    orderLog.getPreviousStatus(),
                    orderLog.getCurrentStatus(),
                    orderLog.getResult(),
                    orderLog.getFailureReason());

            // 이벤트 발행
            eventPublisher.publishEvent(orderLogEvent);

            return savedOrder;

        } catch (Exception e) {
            // 실패 이벤트 객체 생성
            OrderLogEvent orderLogEvent = OrderLogEvent.failure(
                    orderCommand.getUserId(),
                    e.getMessage());

            // 이벤트 발행
            eventPublisher.publishEvent(orderLogEvent);

            throw new RuntimeException("주문 생성에 실패하였습니다. " + e.getMessage());
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
