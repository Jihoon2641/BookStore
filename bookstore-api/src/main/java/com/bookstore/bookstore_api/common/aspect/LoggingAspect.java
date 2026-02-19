package com.bookstore.bookstore_api.common.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.context.request.RequestContextHolder;

@Log4j2
@Aspect
@Component
public class LoggingAspect {

    /**
     * Adapter In (Web Controller) 레이어
     * - 사용자 요청이 들어오는 진입점
     */
    @Pointcut("within(com.bookstore.bookstore_api..adapter.in.web..*)")
    public void webAdapterLayer() {
    }

    /**
     * Application Service 레이어 (UseCase 구현체)
     * - 핵심 비즈니스 로직이 실행되는 곳
     */
    @Pointcut("within(com.bookstore.bookstore_api..application.service..*)")
    public void applicationServiceLayer() {
    }

    /**
     * Scheduler
     * 
     * @param joinPoint
     */
    @Pointcut("within(com.bookstore.bookstore_api..scheduler..*)")
    public void schedulerLayer() {
    }

    @Before("webAdapterLayer()")
    public void logWebRequest(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            MDC.put("requestId", UUID.randomUUID().toString().substring(0, 8));
            MDC.put("uri", request.getMethod() + " " + request.getRequestURI());
            MDC.put("clientIp", getClientIp(request));
            MDC.put("userAgent", request.getHeader("User-Agent"));

            log.info("[Request Start] {}.{} | Params={}", joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
        }
    }

    @AfterReturning(pointcut = "webAdapterLayer()", returning = "result")
    public void logWebResponse(JoinPoint joinPoint, Object result) {
        log.info("[Request End] {}.{} | Response={}", joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(), result != null ? result.getClass().getSimpleName() : "null");
    }

    @Around("applicationServiceLayer()")
    public Object logServiceExecution(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();

        log.info("[Service Start] {}.{} | Params={}", proceedingJoinPoint.getTarget().getClass().getSimpleName(),
                methodName, Arrays.toString(proceedingJoinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis() - startTime;

            log.info("[Service Success] {}.{} | {}ms", proceedingJoinPoint.getTarget().getClass().getSimpleName(),
                    methodName, endTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis() - startTime;
            log.error("[Service Error] {}.{} | Exception={} | Message={} - {}ms",
                    proceedingJoinPoint.getTarget().getClass().getSimpleName(),
                    methodName, e.getClass().getSimpleName(), e.getMessage(), endTime);
            throw e;
        }
    }

    @Around("schedulerLayer()")
    public Object logSchedulerExecution(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String methodName = proceedingJoinPoint.getSignature().getName();

        log.info("[Scheduler Start] {}.{} | Params={}", proceedingJoinPoint.getTarget().getClass().getSimpleName(),
                methodName, Arrays.toString(proceedingJoinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = proceedingJoinPoint.proceed();

            long endTime = System.currentTimeMillis() - startTime;

            log.info("[Scheduler Success] {}.{} | {}ms", proceedingJoinPoint.getTarget().getClass().getSimpleName(),
                    methodName, endTime);

            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis() - startTime;
            log.error("[Scheduler Error] {}.{} | Exception={} | Message={} - {}ms",
                    proceedingJoinPoint.getTarget().getClass().getSimpleName(),
                    methodName, e.getClass().getSimpleName(), e.getMessage(), endTime);
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
