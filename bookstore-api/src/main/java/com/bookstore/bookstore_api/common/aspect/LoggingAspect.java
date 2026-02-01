package com.bookstore.bookstore_api.common.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.ProceedingJoinPoint;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.context.request.RequestContextHolder;

@Log4j2
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.bookstore.bookstore_api..*.*(..))")
    private void allPackage() {
    }

    @Around("allPackage()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // resolveToken 메서드는 로그에서 제외하여 노이즈 감소
        if ("resolveToken".equals(methodName)) {
            return joinPoint.proceed();
        }

        // 1. 요청 정보 추출
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String protocol = "N/A";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            protocol = request.getHeader("X-Forwarded-Proto");
        }

        // 2. 시작 시간 기록
        long start = System.currentTimeMillis();

        log.info("[START] Class: {} | Method: {} | Protocol: {}", className, methodName, protocol);

        try {
            // 3. 비즈니스 로직 실행
            Object proceed = joinPoint.proceed();

            // 4. 종료 시간 기록
            long executionTime = System.currentTimeMillis() - start;

            log.info("[END] Class: {} | Method: {} | Execution Time: {}ms | Result: {}",
                    className, methodName, executionTime, proceed);

            return proceed;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("[ERROR] Class: {} | Method: {} | Execution Time: {}ms | Exception: {}",
                    className, methodName, executionTime, e.getMessage());
            throw e;
        }

    }
}
