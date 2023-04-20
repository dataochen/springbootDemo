package org.egg.rateLimiter.spring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.egg.rateLimiter.annotation.RateLimiter;
import org.egg.rateLimiter.client.RateLimiterClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * Description:
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 13:38
 */
@Component
@Aspect
@Slf4j
public class RateLimiterSpringAop {
    @Pointcut("@annotation(org.egg.rateLimiter.annotation.RateLimiter)")
    public void pointcut() {
    }

    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Signature signature = point.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        if (!rateLimiterClient.canDo(rateLimiter.sourceKey())) {
            // TODO: 2023/4/20 日志
//            log.warn("资源:{} 超限，限速器max:{}", rateLimiter.sourceKey(), rateLimiter.max());
            throw new IllegalStateException("限速器超限");
        }
        return point.proceed();
    }

    @Resource
    private RateLimiterClient rateLimiterClient;
}
