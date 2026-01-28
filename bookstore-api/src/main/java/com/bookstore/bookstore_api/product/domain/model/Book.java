package com.bookstore.bookstore_api.product.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Schema(description = "도서 정보")
@Getter
@AllArgsConstructor
public class Book {

    @Schema(description = "도서 ID")
    private Long id;
    @Schema(description = "도서 제목")
    private String title;
    @Schema(description = "도서 저자")
    private String author;
    @Schema(description = "도서 출판사")
    private String publisher;
    @Schema(description = "도서 ISBN")
    private String isbn;
    @Schema(description = "도서 가격")
    private Long price;
    @Schema(description = "도서 이미지 URL")
    private String imageUrl;
    @Schema(description = "도서 설명")
    private String description;
    @Schema(description = "도서 출판일")
    private LocalDateTime publishedDate;
    @Schema(description = "도서 생성일")
    private LocalDateTime createdAt;
    @Schema(description = "도서 수정일")
    private LocalDateTime updatedAt;

    /**
     * 신규 도서 생성
     * @param title 도서 제목
     * @param author 도서 저자
     * @param publisher 도서 출판사
     * @param isbn 도서 ISBN
     * @param price 도서 가격
     * @param imageUrl 도서 이미지 URL
     * @param description 도서 설명
     * @param publishedDate 도서 출판일
     * @return 신규 도서 정보
     */
    public static Book create(String title, String author, String publisher, 
        String isbn, Long price, String imageUrl, String description, LocalDateTime publishedDate) {
            validateTitle(title);
            validateAuthor(author);
            validatePublisher(publisher);
            validateIsbn(isbn);
            validatePrice(price);
            validateImageUrl(imageUrl);
            validateDescription(description);
            validatePublishedDate(publishedDate);

            return new Book(null, title, author, publisher, isbn, price, imageUrl, description, publishedDate, LocalDateTime.now(), LocalDateTime.now());
        }

    /* =============== 검증 메서드 =============== */

    /**
     * 도서 제목 유효성 검사
     * @param title 도서 제목
     */
    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("도서 제목은 필수입니다.");
        }
    }

    /**
     * 도서 저자 유효성 검사
     * @param author 도서 저자
     */
    private static void validateAuthor(String author) {
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("도서 저자는 필수입니다.");
        }
    }

    /**
     * 도서 출판사 유효성 검사
     * @param publisher 도서 출판사
     */
    private static void validatePublisher(String publisher) {
        if (publisher == null || publisher.isBlank()) {
            throw new IllegalArgumentException("도서 출판사는 필수입니다.");
        }
    }

    /**
     * 도서 ISBN 유효성 검사
     * @param isbn 도서 ISBN
     */
    private static void validateIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("도서 ISBN은 필수입니다.");
        }
    }

    /**
     * 도서 가격 유효성 검사
     * @param price 도서 가격
     */
    private static void validatePrice(Long price) {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("도서 가격은 필수이며 0 이상이어야 합니다.");
        }
    }

    /**
     * 도서 이미지 URL 유효성 검사
     * @param imageUrl 도서 이미지 URL
     */
    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("도서 이미지 URL은 필수입니다.");
        }
    }

    /**
     * 도서 설명 유효성 검사
     * @param description 도서 설명
     */
    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("도서 설명은 필수입니다.");
        }
    }

    /**
     * 도서 출판일 유효성 검사
     * @param publishedDate 도서 출판일
     */
    private static void validatePublishedDate(LocalDateTime publishedDate) {
        if (publishedDate == null) {
            throw new IllegalArgumentException("도서 출판일은 필수입니다.");
        }
    }
}
