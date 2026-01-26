package com.kathena.backend.global.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.kathena.backend.domain..*Controller.*(..))")
    public void allControllers() {}

    @Around("allControllers()")
    public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) traceId = "NO_TRACE_ID";

        HttpServletRequest request = null;
        HttpServletResponse response = null;

        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes sra) {
            request = sra.getRequest();
            response = sra.getResponse();
        }

        String method = (request != null) ? request.getMethod() : "N/A";
        String uri = (request != null) ? request.getRequestURI() : joinPoint.getSignature().toShortString();

        try {
            log.info("[REQUEST][{}] {} {}", traceId, method, uri);

            Object result = joinPoint.proceed();

            long elapsed = System.currentTimeMillis() - start;
            int status = (response != null) ? response.getStatus() : 0;
            log.info("[RESPONSE][{}] {} {} status={} ({}ms)", traceId, method, uri, status, elapsed);

            return result;

        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            int status = (response != null) ? response.getStatus() : 0;

            log.error("[ERROR][{}] {} {} status={} ({}ms) - {}: {}",
                    traceId, method, uri, status, elapsed, e.getClass().getSimpleName(), e.getMessage(), e);

            throw e;
        }
    }
}
