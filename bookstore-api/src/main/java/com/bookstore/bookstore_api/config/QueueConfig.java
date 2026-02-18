package com.bookstore.bookstore_api.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    @Bean
    public BlockingQueue<Long> orderLogQueue() {
        return new ArrayBlockingQueue<>(1000);
    }

}
