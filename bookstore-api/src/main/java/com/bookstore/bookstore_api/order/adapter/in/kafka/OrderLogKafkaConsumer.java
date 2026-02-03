package com.bookstore.bookstore_api.order.adapter.in.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bookstore.bookstore_api.config.KafkaConfig;
import com.bookstore.bookstore_api.order.application.event.message.OrderLogMessage;
import com.bookstore.bookstore_api.order.application.port.out.OrderLogRepository;
import com.bookstore.bookstore_api.order.domain.model.OrderLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLogKafkaConsumer {

    private final OrderLogRepository orderLogRepository;

    /**
     * Kafka에서 주문 로그 메시지 수신 및 DB 저장
     * 
     * @param message 주문 로그 메시지
     */
    @Transactional
    @KafkaListener(
            topics = KafkaConfig.ORDER_LOG_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(OrderLogMessage message) {
        log.info("Kafka 메시지 수신 - orderId: {}, userId: {}, result: {}",
                message.getOrderId(),
                message.getUserId(),
                message.getResult());

        try {
            OrderLog orderLog = convertToOrderLog(message);
            orderLogRepository.save(orderLog);
            log.info("주문 로그 저장 완료 - orderId: {}", message.getOrderId());
        } catch (Exception e) {
            log.error("주문 로그 저장 실패 - orderId: {}, error: {}",
                    message.getOrderId(),
                    e.getMessage());
            // 실패 시 재처리를 위해 예외를 던져 Kafka가 재시도하도록 함
            throw e;
        }
    }

    /**
     * OrderLogMessage를 OrderLog 도메인 객체로 변환
     */
    private OrderLog convertToOrderLog(OrderLogMessage message) {
        if (message.getOrderId() != null) {
            return OrderLog.create(
                    message.getOrderId(),
                    message.getUserId(),
                    message.getPreviousStatus(),
                    message.getCurrentStatus(),
                    message.getResult(),
                    message.getFailureReason());
        } else {
            return OrderLog.createFailure(
                    message.getUserId(),
                    message.getFailureReason());
        }
    }
}
