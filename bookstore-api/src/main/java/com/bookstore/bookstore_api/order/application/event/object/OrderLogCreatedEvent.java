package com.bookstore.bookstore_api.order.application.event.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderLogCreatedEvent {

    private final Long outboxId;

}
