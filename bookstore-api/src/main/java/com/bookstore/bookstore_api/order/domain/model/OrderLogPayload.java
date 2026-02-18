package com.bookstore.bookstore_api.order.domain.model;

import java.time.LocalDateTime;

import com.bookstore.bookstore_api.order.domain.entity.OrderLogEntity;
import com.bookstore.bookstore_api.order.domain.entity.OrderResult;
import com.bookstore.bookstore_api.order.domain.entity.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLogPayload {

    private Long orderId;
    private Long userId;
    private OrderStatus previousStatus;
    private OrderStatus currentStatus;
    private OrderResult result;
    private String failureReason;
    private String requestId;
    private LocalDateTime occurredAt;

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    public static OrderLogPayload fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(json, OrderLogPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize payload", e);
        }
    }

    public OrderLogEntity toEntity() {
        return OrderLogEntity.builder()
                .orderId(this.orderId)
                .userId(this.userId)
                .previousStatus(this.previousStatus)
                .currentStatus(this.currentStatus)
                .result(this.result)
                .failureReason(this.failureReason)
                .requestId(this.requestId)
                .build();
    }
}
