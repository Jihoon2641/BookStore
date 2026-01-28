package com.bookstore.bookstore_api.user.adapter.out.persistence;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookstore.bookstore_api.user.domain.entity.UserEntity;

@Mapper
public interface UserAccountMapper {

    Optional<UserEntity> findByEmail(@Param("email") String email);
    int save(UserEntity userEntity);

} 