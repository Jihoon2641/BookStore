package com.bookstore.bookstore_api.book_api.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.bookstore.bookstore_api.book_api.response.NaverBookResponse;
import com.bookstore.bookstore_api.product.adapter.out.persistence.ProductConverter;
import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import com.bookstore.bookstore_api.product.domain.entity.BookEntity;
import com.bookstore.bookstore_api.product.domain.model.Book;

@Service
public class BookSearchService {

    private static final Long DEFAULT_PRICE = 20000L;
    private static final Long DEFAULT_STOCK = 5L;
    private static final DateTimeFormatter PUBDATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final WebClient naverWebClient;
    private final ProductRepository productRepository;
    private final ProductConverter productConverter;

    public BookSearchService(WebClient naverWebClient, ProductRepository productRepository, ProductConverter productConverter) {
        this.naverWebClient = naverWebClient;
        this.productRepository = productRepository;
        this.productConverter = productConverter;
    }

    public int searchBooksAndSave(String title) {

        /* Naver API 호출 */
        NaverBookResponse response = naverWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/search/book.json")
                .queryParam("query", title)
                .queryParam("display", 100)
                .build())
            .header("X-Naver-Client-Id", clientId)
            .header("X-Naver-Client-Secret", clientSecret)
            .retrieve()
            .bodyToMono(NaverBookResponse.class)
            .block();
        
        /* 도서 정보 저장 */
        if (response != null && response.getItems() != null) {
            List<BookEntity> entities = response.getItems().stream()
                .map(item -> {
                    Long price = parsePrice(item.getDiscount());
                    LocalDateTime publishedDate = parsePublishedDate(item.getPubdate());
                    
                    Book book = Book.create(
                        item.getTitle(), 
                        item.getAuthor(), 
                        item.getPublisher(), 
                        item.getIsbn(), 
                        DEFAULT_STOCK,
                        price, 
                        item.getImage(), 
                        item.getDescription(), 
                        publishedDate);
                    
                    return productConverter.toEntity(book);
                })
                .toList();

            int result = productRepository.saveAll(entities);
            return result;
        }
        return 0;
    }

    /* 가격이 0일 경우 기본 가격(20000원)으로 설정 */
    private Long parsePrice(Integer discount) {
        if (discount == null || discount == 0) {
            return DEFAULT_PRICE;
        }
        return discount.longValue();
    }

    /* 출판일이 null일 경우 null로 설정 및 String -> LocalDateTime 변환  */
    private LocalDateTime parsePublishedDate(String pubdate) {
        if (pubdate == null || pubdate.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(pubdate, PUBDATE_FORMATTER).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }
}
