package org.egg.rateLimiter.spring;

import com.google.common.collect.Maps;
import org.egg.rateLimiter.annotation.RateLimiter;
import org.egg.rateLimiter.mode.SourceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Description:扫描spring的bean 获取注解RateLimiter的内容
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 14:41
 */
@Component
public class AnnotationWrapBean implements BeanPostProcessor {
    /**
     * 本地限速器信息
     * key：资源key
     * value：限速器max值
     */
    private static Map<String, SourceConfig> localRateLimiterDataMap = Maps.newHashMap();

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        Method[] declaredMethods = o.getClass().getDeclaredMethods();
        Arrays.stream(declaredMethods).forEach(method -> {
            RateLimiter annotation = AnnotationUtils.getAnnotation(method, RateLimiter.class);
            if (Objects.nonNull(annotation)) {
                //  同一个资源 配置不同max的校验
                SourceConfig sourceConfig = localRateLimiterDataMap.get(annotation.sourceKey());
                if (Objects.nonNull(sourceConfig) && !Objects.equals(sourceConfig.getMax(), annotation.max())) {
                    throw new IllegalArgumentException(String.format("启动失败，限速器不支持同一个资源 %s 不同的限速值，请检查配置", annotation.sourceKey()));
                }
                localRateLimiterDataMap.put(annotation.sourceKey(), SourceConfig.builder().max(annotation.max())
                        .localLimit(annotation.localLimit()).sourceKey(annotation.sourceKey()).build());
            }
        });
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {

        return o;
    }

    public static Map<String, SourceConfig> getLocalRateLimiterDataMap() {
        return localRateLimiterDataMap;
    }
}
