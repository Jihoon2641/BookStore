package com.bookstore.bookstore_api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;

import com.bookstore.bookstore_api.order.application.event.object.OrderLogCreatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumConfig implements ApplicationRunner {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${debezium.name}")
    private String connectorName;
    @Value("${debezium.database.hostname}")
    private String dbHostname;
    @Value("${debezium.database.port}")
    private String dbPort;
    @Value("${debezium.database.name}")
    private String dbName;
    @Value("${debezium.database.username}")
    private String dbUsername;
    @Value("${debezium.database.password}")
    private String dbPassword;
    @Value("${debezium.database.server-id}")
    private String dbServerId;
    @Value("${debezium.table.include-list}")
    private String tableIncludeList;
    @Value("${debezium.offset.storage-file}")
    private String offsetStorageFile;
    @Value("${debezium.history.file}")
    private String historyFile;

    private DebeziumEngine<ChangeEvent<String, String>> engine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void run(ApplicationArguments args) {
        Properties props = new Properties();

        props.setProperty("name", connectorName);
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");

        // DB 연결 설정
        props.setProperty("database.hostname", dbHostname);
        props.setProperty("database.port", dbPort);
        props.setProperty("database.user", dbUsername);
        props.setProperty("database.password", dbPassword);
        props.setProperty("database.server.id", dbServerId);
        props.setProperty("database.include.list", dbName);

        // 감시할 테이블
        props.setProperty("table.include.list", tableIncludeList);

        // Kafka 없이 파일로 offset 저장
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        props.setProperty("offset.storage.file.filename", offsetStorageFile);
        props.setProperty("offset.flush.interval.ms", "1000");

        // Schema history (Kafka 없이 파일로)
        props.setProperty("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory");
        props.setProperty("schema.history.internal.file.filename", historyFile);

        // 앱 시작 시 기존 데이터 재처리 방지
        props.setProperty("snapshot.mode", "schema_only");

        // topic prefix (내부 식별자용)
        props.setProperty("topic.prefix", "bookstore");

        engine = DebeziumEngine.create(Json.class)
                .using(props)
                .notifying(this::handleEvent)
                .build();

        executor.submit(engine);
        log.info("Debezium CDC 엔진 시작");
    }

    private void handleEvent(ChangeEvent<String, String> event) {

        if (event.value() == null)
            return;

        try {
            JsonNode root = objectMapper.readTree(event.value());
            JsonNode payload = root.path("payload");
            if (payload.isMissingNode() || payload.isNull())
                return;

            JsonNode opNode = payload.get("op");
            if (opNode == null || opNode.isNull())
                return;
            String op = opNode.asText();

            JsonNode after = payload.get("after");
            if (after == null || after.isNull())
                return;

            JsonNode statusNode = after.get("status");
            JsonNode outboxIdNode = after.get("id");
            if (statusNode == null || statusNode.isNull() || outboxIdNode == null || outboxIdNode.isNull())
                return;

            String status = statusNode.asText();
            Long outboxId = outboxIdNode.asLong();

            // INSERT 또는 UPDATE -> PENDING 으로 변경된 경우만 처리
            if ("c".equals(op) || ("u".equals(op) && "PENDING".equals(status))) {
                log.info("Debezium 이벤트 감지 - op: {}, outboxId: {}, status: {}", op, outboxId, status);
                eventPublisher.publishEvent(new OrderLogCreatedEvent(outboxId));
            }
        } catch (Exception e) {
            log.error("Debezium 이벤트 처리 중 에러", e);
        }
    }

    @PreDestroy
    public void stop() throws IOException {
        if (engine != null) {
            engine.close();
        }
        executor.shutdown();
        log.info("Debezium CDC 엔진 종료");
    }

}
