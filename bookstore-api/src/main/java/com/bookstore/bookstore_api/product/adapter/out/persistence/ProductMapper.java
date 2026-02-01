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

    List<BookEntity> findAllByIds(@Param("ids") List<Long> ids);

    BookEntity findByIdWithLock(@Param("id") Long id);

    int updateStock(@Param("stockDecreaseCommands") List<StockDecreaseCommand> stockDecreaseCommands);

    int saveAll(@Param("books") List<BookEntity> books);

    int syncPopularStatus();
}
