package com.bookstore.bookstore_api.user.adapter.out.persistence;

import com.bookstore.bookstore_api.user.application.port.out.UserLogRepository;
import com.bookstore.bookstore_api.user.domain.entity.UserLogEntity;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 로그 Repository Adapter (MyBatis)
 */
@Component
@RequiredArgsConstructor
public class UserLogAdapterMybatis implements UserLogRepository {

    private final UserLogMapper userLogMapper;

    @Override
    public UserLogEntity save(UserLogEntity userLog) {
        int result = userLogMapper.save(userLog);
        if (result == 0) {
            throw new RuntimeException("로그 저장에 실패했습니다.");
        }
        return userLog;
    }

    @Override
    public long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after) {
        return userLogMapper.countByIpAddressAndCreatedAtAfter(ipAddress, after);
    }

    @Override
    public List<UserLogEntity> findByIpAddressAndIsAnomalousTrueOrderByCreatedAtDesc(String ipAddress) {
        return userLogMapper.findByIpAddressAndIsAnomalousTrueOrderByCreatedAtDesc(ipAddress);
    }

    @Override
    public List<UserLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return userLogMapper.findByUserIdOrderByCreatedAtDesc(userId);
    }

}
