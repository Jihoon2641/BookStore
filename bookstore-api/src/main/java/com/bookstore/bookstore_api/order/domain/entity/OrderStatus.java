package com.bookstore.bookstore_api.order.domain.entity;

public enum OrderStatus {
    PENDING("대기", "주문이 접수되었습니다."),
    CONFIRMED("확정", "주문이 확정되었습니다."),
    SHIPPED("배송중", "상품이 배송중입니다."),
    DELIVERED("배송완료", "상품이 배송완료되었습니다."),
    CANCELLED("취소", "주문이 취소되었습니다.");

    private final String name;
    private final String description;

    OrderStatus(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}