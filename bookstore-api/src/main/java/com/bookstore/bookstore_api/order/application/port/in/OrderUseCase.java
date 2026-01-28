package com.bookstore.bookstore_api.order.application.port.in;

import com.bookstore.bookstore_api.order.domain.model.Orders;

public interface OrderUseCase {

    /**
     * 주문 생성
     * @param orderRequest 주문 요청
     * @return 주문 생성 결과
     */
    Orders createOrder(OrderCommand orderRequest);

}