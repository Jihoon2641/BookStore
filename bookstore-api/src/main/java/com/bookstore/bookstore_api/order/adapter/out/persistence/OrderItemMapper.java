package com.bookstore.bookstore_api.order.adapter.out.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookstore.bookstore_api.order.domain.entity.OrderItemEntity;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    /**
     * 주문 항목 일괄 저장
     * @param orderItems 주문 항목 엔티티 리스트
     * @return 저장된 행 수
     */
    int saveAll(@Param("orderItems") List<OrderItemEntity> orderItems);

    /**
     * 주문 ID로 주문 항목 조회
     * @param orderId 주문 ID
     * @return 주문 항목 엔티티 리스트
     */
    List<OrderItemEntity> findByOrderId(@Param("orderId") Long orderId);

}
