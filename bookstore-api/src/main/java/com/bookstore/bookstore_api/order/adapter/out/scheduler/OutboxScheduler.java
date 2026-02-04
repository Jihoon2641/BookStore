package com.bookstore.bookstore_api.order.adapter.out.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.config.KafkaConfig;
import com.bookstore.bookstore_api.order.application.event.message.OrderLogMessage;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogOutboxRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderLogOutBox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;

/**
 * Outbox 테이블을 폴링하여 Kafka로 메시지를 발행하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OrderLogOutboxRepository orderLogOutboxRepository;
    private final KafkaTemplate<String, OrderLogMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** 한 번에 처리할 최대 레코드 수 */
    private static final int BATCH_SIZE = 100;

    /** 최대 재시도 횟수 */
    private static final int MAX_RETRY = 3;

    /**
     * 5초마다 PENDING 상태의 Outbox 레코드를 조회하여 Kafka로 발행
     */
    @Scheduled(fixedDelay = 5000)
    public void publishPendingOutbox() {
        List<OrderLogOutBox> pendingList = orderLogOutboxRepository.findPendingWithLimit(BATCH_SIZE);

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("Outbox 스케줄러 시작 - 처리 대상: {}건", pendingList.size());

        for (OrderLogOutBox outbox : pendingList) {
            processOutbox(outbox);
        }

        log.info("Outbox 스케줄러 완료");
    }

    /**
     * 개별 Outbox 레코드 처리
     * 
     * @param outbox Outbox 레코드
     */
    private void processOutbox(OrderLogOutBox outbox) {
        try {
            OrderLogMessage message = deserializePayload(outbox.getPayload());

            String key = message.getOrderId() != null
                    ? String.valueOf(message.getOrderId())
                    : String.valueOf(message.getUserId());

            kafkaTemplate.send(KafkaConfig.ORDER_LOG_TOPIC, key, message).get();

            orderLogOutboxRepository.markAsSent(outbox.getId());

            log.info("Outbox 발행 성공 - id: {}, orderId: {}", outbox.getId(), outbox.getOrderId());

        } catch (Exception e) {

            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            orderLogOutboxRepository.markAsFailed(outbox.getId(), errorMessage, MAX_RETRY);

            log.error("Outbox 발행 실패 - id: {}, error: {}", outbox.getId(), errorMessage);
        }
    }

    /**
     * JSON payload를 OrderLogMessage로 역직렬화
     * 
     * @param payload JSON 문자열
     * @return OrderLogMessage 객체
     */
    private OrderLogMessage deserializePayload(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, OrderLogMessage.class);
    }
}
