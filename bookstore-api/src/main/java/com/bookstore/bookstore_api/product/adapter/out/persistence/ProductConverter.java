package com.bookstore.bookstore_api.product.adapter.out.persistence;

import org.mapstruct.Mapper;
import com.bookstore.bookstore_api.product.domain.entity.BookEntity;
import com.bookstore.bookstore_api.product.domain.model.Book;

@Mapper(componentModel = "spring")
public interface ProductConverter {

    BookEntity toEntity(Book book);

    Book toModel(BookEntity bookEntity);

}
