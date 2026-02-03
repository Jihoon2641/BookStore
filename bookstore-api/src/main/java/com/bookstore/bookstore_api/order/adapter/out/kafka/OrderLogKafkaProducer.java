package com.bookstore.bookstore_api.order.adapter.out.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.config.KafkaConfig;
import com.bookstore.bookstore_api.order.application.event.message.OrderLogMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderLogKafkaProducer {

    private final KafkaTemplate<String, OrderLogMessage> kafkaTemplate;

    /**
     * 주문 로그 메시지를 Kafka로 발행
     * 
     * @param message 주문 로그 메시지
     */
    public void send(OrderLogMessage message) {
        String key = message.getOrderId() != null 
                ? String.valueOf(message.getOrderId()) 
                : String.valueOf(message.getUserId());

        CompletableFuture<SendResult<String, OrderLogMessage>> future = 
                kafkaTemplate.send(KafkaConfig.ORDER_LOG_TOPIC, key, message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka 메시지 발행 성공 - topic: {}, key: {}, offset: {}",
                        KafkaConfig.ORDER_LOG_TOPIC,
                        key,
                        result.getRecordMetadata().offset());
            } else {
                log.error("Kafka 메시지 발행 실패 - topic: {}, key: {}, error: {}",
                        KafkaConfig.ORDER_LOG_TOPIC,
                        key,
                        ex.getMessage());
            }
        });
    }
}
