package com.bookstore.bookstore_api.order.domain.entity;

public enum OrderResult {
    SUCCESS("성공", "주문 처리가 성공했습니다."),
    FAILURE("실패", "주문 처리가 실패했습니다.");

    private final String name;
    private final String description;

    OrderResult(String name, String description) {
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
