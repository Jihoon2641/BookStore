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
    @Schema(description = "요청 ID")
    private String requestId;

    /**
     * OrderLogOutBox 생성
     * 
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
    public static OrderLogOutBox create(Long id, Long orderId, Long userId, String payload, OutboxStatus status,
            LocalDateTime createdAt, LocalDateTime sentAt, int retryCount, String lastError, String requestId) {
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
                .requestId(requestId)
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
    public static OrderLogOutBox pending(Long orderId, Long userId, String payload, String requestId) {
        return OrderLogOutBox.builder()
                .orderId(orderId)
                .userId(userId)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .requestId(requestId)
                .build();
    }

    /**
     * Outbox 레코드를 SENT 상태로 변경
     * 
     * @param sentAt 전송일시
     * @return OrderLogOutBox
     */
    public OrderLogOutBox markAsSent(LocalDateTime sentAt) {
        return OrderLogOutBox.builder()
                .id(this.id)
                .orderId(this.orderId)
                .userId(this.userId)
                .payload(this.payload)
                .requestId(this.requestId)
                .status(OutboxStatus.SENT)
                .createdAt(this.createdAt)
                .sentAt(sentAt)
                .retryCount(this.retryCount)
                .lastError(this.lastError)
                .build();
    }

    /**
     * Outbox 레코드를 FAILED 상태로 변경
     * 
     * @param lastError 마지막 에러
     * @return OrderLogOutBox
     */
    public OrderLogOutBox markAsFailed(String lastError) {
        return OrderLogOutBox.builder()
                .id(this.id)
                .orderId(this.orderId)
                .userId(this.userId)
                .payload(this.payload)
                .requestId(this.requestId)
                .status(OutboxStatus.FAILED)
                .createdAt(this.createdAt)
                .sentAt(this.sentAt)
                .retryCount(this.retryCount)
                .lastError(lastError)
                .build();
    }

    /**
     * 재시도 횟수 증가
     * 
     * @return OrderLogOutBox
     */
    public OrderLogOutBox incrementRetryCount() {
        return OrderLogOutBox.builder()
                .id(this.id)
                .orderId(this.orderId)
                .userId(this.userId)
                .payload(this.payload)
                .requestId(this.requestId)
                .status(this.status)
                .createdAt(this.createdAt)
                .sentAt(this.sentAt)
                .retryCount(this.retryCount + 1)
                .lastError(this.lastError)
                .build();
    }

}
