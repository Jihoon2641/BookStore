package com.bookstore.bookstore_api.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.bookstore.bookstore_api.util.domain.BaseEntity;

@Entity
@Table(name = "books")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class BookEntity extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private String publisher;

    @Column(unique = true)
    private String isbn;

    private Long price;
    private Long stock;
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

}
