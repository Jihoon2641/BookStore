package com.bookstore.bookstore_api.util.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Column;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // @CreatedBy
    // @Column(name = "created_by", nullable = false, updatable = false)
    // private String createdBy;

    // @LastModifiedBy
    // @Column(name = "updated_by", nullable = false)
    // private String updatedBy;
}
