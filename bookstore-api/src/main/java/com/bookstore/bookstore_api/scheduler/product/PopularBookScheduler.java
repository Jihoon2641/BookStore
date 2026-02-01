package com.bookstore.bookstore_api.scheduler.product;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PopularBookScheduler {

    private final ProductRepository productRepository;

    /**
     * 인기 상품 상태 업데이트
     * is_popular 컬럼 변경
     */
    @Scheduled(initialDelay = 10000, fixedDelay = 60 * 60 * 1000)
    public void runSyncPopularStatus() {
        productRepository.syncPopularStatus();
    }
}
