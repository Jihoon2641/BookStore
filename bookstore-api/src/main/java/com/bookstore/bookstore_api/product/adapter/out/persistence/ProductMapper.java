package com.bookstore.bookstore_api.product.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;

import com.bookstore.bookstore_api.product.domain.entity.BookEntity;

@Mapper
public interface ProductMapper {

    BookEntity findById(Long id);

}
