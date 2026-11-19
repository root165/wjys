package com.wjys.common.aspect;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.trace.TraceIdContext;
import com.wjys.module.system.service.OperLogService;
import com.wjys.security.WjysPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {
    private final OperLogService logService;

    @Around("@annotation(operLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        WjysPrincipal principal = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof WjysPrincipal current) principal = current;
        boolean success = false;
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } finally {
            logService.record(principal == null ? null : principal.id(), principal == null ? null : principal.username(), operLog.value(), request.getMethod(), request.getRequestURI(), success, TraceIdContext.current());
        }
    }
}
