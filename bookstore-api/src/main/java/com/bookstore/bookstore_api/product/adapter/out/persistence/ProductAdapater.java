package com.bookstore.bookstore_api.product.adapter.out.persistence;

import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

import com.bookstore.bookstore_api.product.domain.model.Book;
import com.bookstore.bookstore_api.product.domain.entity.BookEntity;
import com.bookstore.bookstore_api.product.adapter.in.StockDecreaseCommand;

@Repository
@RequiredArgsConstructor
public class ProductAdapater implements ProductRepository {

    private final ProductMapper productMapper;
    private final ProductConverter productConverter;

    @Override
    public Optional<Book> findById(Long id) {
        BookEntity entity = productMapper.findById(id);
        return Optional.ofNullable(entity)
                .map(productConverter::toModel);
    }

    @Override
    public Optional<List<Book>> findAllByIdsWithLock(List<Long> ids) {
        List<BookEntity> entities = productMapper.findAllByIdsWithLock(ids);

        if (entities == null || entities.isEmpty()) {
            return Optional.empty();
        }

        List<Book> books = entities.stream()
                .map(productConverter::toModel)
                .toList();
        return Optional.of(books);
    }

    @Override
    public int updateStock(List<StockDecreaseCommand> stockDecreaseCommands) {
        return productMapper.updateStock(stockDecreaseCommands);
    }

}
