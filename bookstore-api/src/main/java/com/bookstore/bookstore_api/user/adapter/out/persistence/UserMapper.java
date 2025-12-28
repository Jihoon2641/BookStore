package com.bookstore.bookstore_api.user.adapter.out.persistence;

import org.mapstruct.Mapper;

import com.bookstore.bookstore_api.user.domain.entity.UserEntity;
import com.bookstore.bookstore_api.user.domain.model.User;

/**
 * 사용자 엔티티와 모델 간의 변환을 담당하는 매퍼
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * User 모델을 UserEntity 엔티티로 변환
     * @param user User 모델
     * @return UserEntity 엔티티
     */
    UserEntity toEntity(User user);

    /**
     * UserEntity 엔티티를 User 모델로 변환
     * @param userEntity UserEntity 엔티티
     * @return User 모델
     */
    User toModel(UserEntity userEntity);

} 