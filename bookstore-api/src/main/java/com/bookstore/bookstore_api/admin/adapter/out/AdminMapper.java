package com.bookstore.bookstore_api.admin.adapter.out;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bookstore.bookstore_api.admin.domain.entity.AdminEntity;

@Mapper
public interface AdminMapper {

    Optional<AdminEntity> findByAdminId(@Param("adminId") String adminId);

    int save(AdminEntity adminEntity);
}
