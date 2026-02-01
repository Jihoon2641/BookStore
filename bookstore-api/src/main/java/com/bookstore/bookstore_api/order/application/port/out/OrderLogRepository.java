package com.bookstore.bookstore_api.order.application.port.out;

import com.bookstore.bookstore_api.order.domain.model.OrderLog;

public interface OrderLogRepository {

    /**
     * 주문 로그 저장
     * @param orderLog 주문 로그
     * @return 저장된 주문 로그
     */
    OrderLog save(OrderLog orderLog);

}
