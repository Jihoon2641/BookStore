package com.bookstore.bookstore_api.order.domain.model;

import java.time.LocalDateTime;

import com.bookstore.bookstore_api.common.outboxEnum.OutboxStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "OrderLogOutBox")
@Getter
@Builder
public class OrderLogOutBox {

    @Schema(description = "ID")
    private Long id;
    @Schema(description = "주문 ID")
    private Long orderId;
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "본문 내용")
    private String payload;
    @Schema(description = "상태")
    private OutboxStatus status;
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;
    @Schema(description = "전송일시")
    private LocalDateTime sentAt;
    @Schema(description = "재시도 횟수")
    private int retryCount;
    @Schema(description = "마지막 에러")
    private String lastError;

    /**
     * OrderLogOutBox 생성
     * @param id
     * @param orderId
     * @param userId
     * @param payload
     * @param status
     * @param createdAt
     * @param sentAt
     * @param retryCount
     * @param lastError
     * @return
     */
    public static OrderLogOutBox create(Long id, Long orderId, Long userId, String payload, OutboxStatus status, LocalDateTime createdAt, LocalDateTime sentAt, int retryCount, String lastError) {
        return OrderLogOutBox.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .payload(payload)
                .status(status)
                .createdAt(createdAt)
                .sentAt(sentAt)
                .retryCount(retryCount)
                .lastError(lastError)
                .build();
    }

    /**
     * 신규 Outbox 레코드 생성 : PENDING 상태
     * 
     * @param orderId 주문 ID
     * @param userId  사용자 ID
     * @param payload JSON payload
     * @return OrderLogOutBox
     */
    public static OrderLogOutBox pending(Long orderId, Long userId, String payload) {
        return OrderLogOutBox.builder()
                .orderId(orderId)
                .userId(userId)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }

    /**
     * 신규 Outbox 레코드 생성 : FAILED 상태
     * 
     * @param orderId 주문 ID
     * @param userId  사용자 ID
     * @param payload JSON payload
     * @param lastError 마지막 에러
     * @return OrderLogOutBox
     */
    public static OrderLogOutBox failed(Long orderId, Long userId, String payload, String lastError) {
        return OrderLogOutBox.builder()
                .orderId(orderId)
                .userId(userId)
                .payload(payload)
                .status(OutboxStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .lastError(lastError)
                .build();
    }

    /**
     * 신규 Outbox 레코드 생성 : SENT 상태
     * 
     * @param orderId 주문 ID
     * @param userId  사용자 ID
     * @param payload JSON payload
     * @return OrderLogOutBox
     */
    public static OrderLogOutBox sent(Long orderId, Long userId, String payload) {
        return OrderLogOutBox.builder()
                .orderId(orderId)
                .userId(userId)
                .payload(payload)
                .status(OutboxStatus.SENT)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .retryCount(0)
                .build();
    }
}
