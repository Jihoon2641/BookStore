package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;

import com.bookstore.bookstore_api.order.domain.entity.OrderLogEntity;

@Mapper
public interface OrderLogMapper {

    /**
     * 주문 로그 저장
     * @param orderLogEntity 주문 로그 엔티티
     * @return 저장된 행 수
     */
    int save(OrderLogEntity orderLogEntity);

}
