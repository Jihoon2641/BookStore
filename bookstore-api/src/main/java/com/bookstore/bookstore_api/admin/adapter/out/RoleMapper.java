package com.bookstore.bookstore_api.admin.adapter.out;

import org.apache.ibatis.annotations.Mapper;

import com.bookstore.bookstore_api.admin.domain.entity.RolesEntity;

@Mapper
public interface RoleMapper {

    RolesEntity findById(Long id);
}