// package com.bookstore.bookstore_api.order.application;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyList;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.verify;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.bookstore.bookstore_api.order.application.port.in.OrderCommand;
// import
// com.bookstore.bookstore_api.order.application.port.in.OrderItemCommand;
// import
// com.bookstore.bookstore_api.order.application.port.out.OrderItemRepository;
// import
// com.bookstore.bookstore_api.order.application.port.out.OrderLogRepository;
// import
// com.bookstore.bookstore_api.order.application.port.out.OrderRepository;
// import com.bookstore.bookstore_api.order.application.service.OrderService;
// import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;
// import com.bookstore.bookstore_api.order.domain.model.Orders;
// import
// com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
// import com.bookstore.bookstore_api.product.domain.model.Book;

// @ExtendWith(MockitoExtension.class)
// class OrderServiceTest {

// @Mock
// private OrderRepository orderRepository;

// @Mock
// private OrderItemRepository orderItemRepository;

// @Mock
// private OrderLogRepository orderLogRepository;

// @Mock
// private ProductRepository productRepository;

// @InjectMocks
// private OrderService orderService;

// private OrderCommand orderCommand;
// private Book testBook;

// @BeforeEach
// void setUp() {
// OrderItemCommand itemCommand = new OrderItemCommand(1L, 10000L, 2L);
// orderCommand = new OrderCommand(
// 1L,
// List.of(itemCommand));

// testBook = new Book(
// 1L, "테스트 책", "테스트 저자", "테스트 출판사",
// "1234567890123", 15000L, 100L, "http://image.url",
// "테스트 설명", LocalDateTime.now());
// }

// @Test
// @DisplayName("상품이 존재하지 않으면 주문 생성에 실패한다")
// void createOrder_failsWhenProductNotFound() {
// // given
// given(productRepository.findAllByIdsWithLock(anyList()))
// .willReturn(Optional.empty());

// // when & then
// assertThatThrownBy(() -> orderService.createOrder(orderCommand))
// .isInstanceOf(RuntimeException.class)
// .hasMessageContaining("주문 생성에 실패");

// // 주문 실패 로그가 저장되어야 함
// verify(orderLogRepository).save(any());
// // 주문은 저장되지 않아야 함
// verify(orderRepository, never()).save(any());
// }

// @Test
// @DisplayName("재고가 부족하면 주문 생성에 실패한다")
// void createOrder_failsWhenStockInsufficient() {
// // given
// Book lowStockBook = new Book(
// 1L, "테스트 책", "테스트 저자", "테스트 출판사",
// "1234567890123", 15000L, 1L, "http://image.url",
// "테스트 설명", LocalDateTime.now());

// given(productRepository.findAllByIdsWithLock(anyList()))
// .willReturn(Optional.of(List.of(lowStockBook)));

// // when & then
// assertThatThrownBy(() -> orderService.createOrder(orderCommand))
// .isInstanceOf(RuntimeException.class)
// .hasMessageContaining("주문 생성에 실패");

// verify(orderLogRepository).save(any());
// verify(orderRepository, never()).save(any());
// }

// @Test
// @DisplayName("주문 생성에 성공하면 주문 정보를 반환한다")
// void createOrder_success() {
// // given
// Orders savedOrder = new Orders(1L, 1L, LocalDateTime.now(),
// OrderStatus.PENDING);

// given(productRepository.findAllByIdsWithLock(anyList()))
// .willReturn(Optional.of(List.of(testBook)));
// given(productRepository.updateStock(anyList()))
// .willReturn(1);
// given(orderRepository.save(any(Orders.class)))
// .willReturn(savedOrder);

// // when
// Orders result = orderService.createOrder(orderCommand);

// // then
// assertThat(result).isNotNull();
// assertThat(result.getId()).isEqualTo(1L);
// assertThat(result.getUserId()).isEqualTo(1L);
// assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);

// verify(orderRepository).save(any(Orders.class));
// verify(orderItemRepository).saveAll(anyList());
// verify(orderLogRepository).save(any());
// }

// @Test
// @DisplayName("주문 저장에 실패하면 예외가 발생한다")
// void createOrder_failsWhenOrderSaveFails() {
// // given
// given(productRepository.findAllByIdsWithLock(anyList()))
// .willReturn(Optional.of(List.of(testBook)));
// given(productRepository.updateStock(anyList()))
// .willReturn(1);
// given(orderRepository.save(any(Orders.class)))
// .willReturn(null);

// // when & then
// assertThatThrownBy(() -> orderService.createOrder(orderCommand))
// .isInstanceOf(RuntimeException.class)
// .hasMessageContaining("주문 생성에 실패");

// verify(orderLogRepository).save(any());
// }
// }
