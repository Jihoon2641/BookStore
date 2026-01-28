package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;

import com.bookstore.bookstore_api.order.domain.entity.OrdersEntity;

@Mapper
public interface OrderMapper {

    int save(OrdersEntity orderEntity);

}
