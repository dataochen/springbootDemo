package org.egg.rateLimiter.annotation;

import java.lang.annotation.*;

/**
 * Description: 方法级上注解 可控制方法调用限速
 * 1.需要扫描指定目录下的class的所有方法上的注解
 * 2.借助spring aop给指定方法增强
 *
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 10:56
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RateLimiter {
    /**
     * 分布式最大限流量
     * @return
     */
    int max() default 1000;

    /**
     * 当控制台（redis)不可用时 降级本地限速器的最大限制值
     * @return
     */
    int localLimit() default 100;

    /**
     * 限流资源key
     * @return
     */
    String sourceKey();

}
