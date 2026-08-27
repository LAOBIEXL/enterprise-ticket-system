package com.example.demo.aspect;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 统一记录所有 Controller 接口的访问日志。
 */
@Aspect
@Component
public class ControllerLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ControllerLogAspect.class);

    /**
     * 匹配 controller 包及其子包中的所有 public 方法。
     */
    @Pointcut("execution(public * com.example.demo.controller..*(..))")
    public void controllerMethod() {
    }

    /**
     * 在 Controller 接口执行前后记录请求信息、执行结果和耗时。
     */
    @Around("controllerMethod()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        String httpMethod = request == null ? "UNKNOWN" : request.getMethod();
        String requestUri = request == null ? "UNKNOWN" : request.getRequestURI();
        String clientIp = request == null ? "UNKNOWN" : request.getRemoteAddr();
        String handler = joinPoint.getSignature().toShortString();
        String arguments = formatArguments(joinPoint.getArgs());
        long startTime = System.nanoTime();

        log.info("Controller 请求开始 | method={} | uri={} | handler={} | clientIp={} | args={}",
                httpMethod, requestUri, handler, clientIp, arguments);

        try {
            Object result = joinPoint.proceed();
            long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
            log.info("Controller 请求结束 | method={} | uri={} | handler={} | elapsedMs={}",
                    httpMethod, requestUri, handler, elapsedMillis);
            return result;
        } catch (Throwable throwable) {
            long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
            log.error("Controller 请求异常 | method={} | uri={} | handler={} | elapsedMs={} | message={}",
                    httpMethod, requestUri, handler, elapsedMillis, throwable.getMessage(), throwable);
            throw throwable;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String formatArguments(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return "[]";
        }
        return Arrays.stream(arguments)
                .filter(argument -> !(argument instanceof ServletRequest))
                .filter(argument -> !(argument instanceof ServletResponse))
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
