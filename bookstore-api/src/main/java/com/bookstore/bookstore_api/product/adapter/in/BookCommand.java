package com.bookstore.bookstore_api.product.adapter.in;

import com.bookstore.bookstore_api.util.validate.SelfValidating;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BookCommand extends SelfValidating<BookCommand> {

    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    private final String title;

    @NotBlank(message = "저자는 필수 입력 항목입니다.")
    private final String author;

    @NotBlank(message = "출판사는 필수 입력 항목입니다.")
    private final String publisher;

    @NotBlank(message = "ISBN은 필수 입력 항목입니다.")
    @Size(min = 10, max = 13, message = "ISBN은 10자 또는 13자여야 합니다.")
    private final String isbn;

    @NotNull(message = "가격은 필수 입력 항목입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private final Long price;

    @NotNull(message = "재고는 필수 입력 항목입니다.")
    @Min(value = 0, message = "재고는 0개 이상이어야 합니다.")
    private final Long stock;

    @NotBlank(message = "이미지 URL은 필수 입력 항목입니다.")
    private final String imageUrl;

    private final String description;

    private final LocalDateTime publishedDate;

    public BookCommand(String title, String author, String publisher, String isbn, 
                       Long price, Long stock, String imageUrl, 
                       String description, LocalDateTime publishedDate) {
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.description = description;
        this.publishedDate = publishedDate;
        this.validateSelf();
    }

}
