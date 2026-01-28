package com.bookstore.bookstore_api.product.adapter.out.persistence;

import com.bookstore.bookstore_api.product.application.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import com.bookstore.bookstore_api.product.domain.model.Book;
import com.bookstore.bookstore_api.product.domain.entity.BookEntity;

@RequiredArgsConstructor
public class ProductAdapater implements ProductRepository{

    private final ProductMapper productMapper;
    private final ProductConverter productConverter;
    
    @Override
    public Optional<Book> findById(Long id) {
        BookEntity entity = productMapper.findById(id);
        return Optional.ofNullable(entity)
                       .map(productConverter::toModel);
    }

}
