package com.bookstore.bookstore_api.order.utils;

import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.order.domain.model.OrderLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxPayloadSerializer {

    private final ObjectMapper objectMapper;

    /**
     * 주문 성공 로그를 JSON payload로 변환
     * 
     * @param orderLog 주문 로그 정보
     * @return JSON 문자열
     */
    public String serializeOrderLog(OrderLog orderLog) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderLog.getOrderId());
        payload.put("userId", orderLog.getUserId());
        payload.put("previousStatus", orderLog.getPreviousStatus() != null 
                ? orderLog.getPreviousStatus().name() : null);
        payload.put("currentStatus", orderLog.getCurrentStatus() != null 
                ? orderLog.getCurrentStatus().name() : null);
        payload.put("result", orderLog.getResult() != null 
                ? orderLog.getResult().name() : null);
        payload.put("failureReason", orderLog.getFailureReason());

        return toJson(payload);
    }

    /**
     * 주문 실패 로그를 JSON payload로 변환
     * 
     * @param userId        사용자 ID
     * @param failureReason 실패 사유
     * @return JSON 문자열
     */
    public String serializeFailureLog(Long userId, String failureReason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", null);
        payload.put("userId", userId);
        payload.put("previousStatus", null);
        payload.put("currentStatus", "FAILED");
        payload.put("result", "FAILURE");
        payload.put("failureReason", failureReason);

        return toJson(payload);
    }

    /**
     * Map을 JSON 문자열로 변환
     * 
     * @param payload 변환할 Map
     * @return JSON 문자열
     */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Payload 직렬화에 실패하였습니다.", e);
        }
    }
}
