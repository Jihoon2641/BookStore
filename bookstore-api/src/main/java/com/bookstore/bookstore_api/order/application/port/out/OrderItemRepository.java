package com.bookstore.bookstore_api.order.application.port.out;

import com.bookstore.bookstore_api.order.domain.model.OrderItem;

import java.util.List;

public interface OrderItemRepository {

    /**
     * 주문 항목 일괄 저장
     * @param orderItems 주문 항목 리스트
     * @return 저장된 행 수
     */
    int saveAll(List<OrderItem> orderItems);

    /**
     * 주문 ID로 주문 항목 조회
     * @param orderId 주문 ID
     * @return 주문 항목 리스트
     */
    List<OrderItem> findByOrderId(Long orderId);

}
