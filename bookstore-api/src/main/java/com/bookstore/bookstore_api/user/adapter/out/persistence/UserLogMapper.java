package com.bookstore.bookstore_api.user.adapter.out.persistence;

import com.bookstore.bookstore_api.user.domain.entity.UserLogEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 로그 MyBatis Mapper
 */
@Mapper
public interface UserLogMapper {

    int save(UserLogEntity userLog);

    long countByIpAddressAndCreatedAtAfter(
            @Param("ipAddress") String ipAddress,
            @Param("after") LocalDateTime after);

    List<UserLogEntity> findByIpAddressAndIsAnomalousTrueOrderByCreatedAtDesc(
            @Param("ipAddress") String ipAddress);

    List<UserLogEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

}
