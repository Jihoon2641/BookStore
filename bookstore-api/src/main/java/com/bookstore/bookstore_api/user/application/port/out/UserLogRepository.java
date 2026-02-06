package com.bookstore.bookstore_api.user.application.port.out;

import com.bookstore.bookstore_api.user.domain.entity.UserLogEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface UserLogRepository {

    UserLogEntity save(UserLogEntity userLog);

    /**
     * 특정 IP의 지정된 시간 이후 요청 수 조회
     */
    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    /**
     * 특정 IP의 이상 패턴 로그 조회
     */
    List<UserLogEntity> findByIpAddressAndIsAnomalousTrueOrderByCreatedAtDesc(String ipAddress);

    /**
     * 특정 사용자의 로그 조회
     */
    List<UserLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

}
