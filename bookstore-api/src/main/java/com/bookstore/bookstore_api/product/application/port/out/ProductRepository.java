package com.bookstore.bookstore_api.product.application.port.out;

import java.util.Optional;

import com.bookstore.bookstore_api.product.domain.model.Book;

public interface ProductRepository {

    Optional<Book> findById(Long id);

}
