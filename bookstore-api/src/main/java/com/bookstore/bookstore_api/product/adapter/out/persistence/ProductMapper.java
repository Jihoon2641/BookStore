package com.bookstore.bookstore_api.product.adapter.out.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookstore.bookstore_api.product.domain.entity.BookEntity;
import com.bookstore.bookstore_api.product.adapter.in.StockDecreaseCommand;

@Mapper
public interface ProductMapper {

    BookEntity findById(@Param("id") Long id);

    List<BookEntity> findAllByIdsWithLock(@Param("ids") List<Long> ids);

    int updateStock(@Param("stockDecreaseCommands") List<StockDecreaseCommand> stockDecreaseCommands);
}
