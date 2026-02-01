package com.bookstore.bookstore_api.product.application.port.out;

import java.util.Optional;
import java.util.List;

import com.bookstore.bookstore_api.product.domain.entity.BookEntity;
import com.bookstore.bookstore_api.product.domain.model.Book;
import com.bookstore.bookstore_api.product.adapter.in.StockDecreaseCommand;

public interface ProductRepository {

    Optional<Book> findById(Long id);

    Optional<List<Book>> findAllByIdsWithLock(List<Long> ids);

    int updateStock(List<StockDecreaseCommand> stockDecreaseCommands);

    int saveAll(List<BookEntity> books);

    int syncPopularStatus();
}
