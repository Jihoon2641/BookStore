package com.bookstore.bookstore_api.book_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;

@Service
public class BookSearchService {

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final WebClient naverWebClient;

    public BookSearchService(WebClient naverWebClient) {
        this.naverWebClient = naverWebClient;
    }

    public String searchBooks(String title) {

        return naverWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/book.json")
                        .queryParam("query", title)
                        .queryParam("display", 10)
                        .build(true))
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
