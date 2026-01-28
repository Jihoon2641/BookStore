package com.bookstore.bookstore_api.order.application.port.out;

import com.bookstore.bookstore_api.order.domain.model.Orders;

public interface OrderRepository {

    /**
     * 주문 생성
     * @param order 주문
     * @return 주문
     */
    Orders save(Orders order);

}
