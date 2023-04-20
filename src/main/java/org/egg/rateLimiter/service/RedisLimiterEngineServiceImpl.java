package org.egg.rateLimiter.service;

import org.egg.rateLimiter.mode.EngineTypeEnum;
import org.egg.rateLimiter.mode.HealthModel;
import org.egg.rateLimiter.mode.IndexData;
import org.egg.utils.JacksonSerializer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Description: redis控制台
 * 后续再考虑 打包成sdk
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 11:13
 */
@Component
public class RedisLimiterEngineServiceImpl implements LimiterEngineService {
    @Override
    public EngineTypeEnum engine() {
        return EngineTypeEnum.REDIS;
    }

    @Override
    public boolean pushIndexData(String sourceKey, IndexData indexData) {
        redis.opsForHash().put(obtainIndexDataKey(sourceKey), indexData.getIp(), JacksonSerializer.serialize(indexData));
        return true;
    }

    @Override
    public List<IndexData> pullIndexDataList(String sourceKey) {
        Map<String, String> indexDataMap = redis.opsForHash().entries(obtainIndexDataKey(sourceKey));
        if (CollectionUtils.isEmpty(indexDataMap)) {
            return new ArrayList<>();
        }
        return indexDataMap.values()
                .stream()
                .map(val -> JacksonSerializer.deSerialize(val, IndexData.class))
                .collect(Collectors.toList());
    }

    @Override
    public boolean pushSourceKey(String sourceKey) {
        redis.opsForSet().add(obtainSourceKey(), sourceKey);
        return true;
    }

    @Override
    public Set<String> pullSourceKeys() {
        return redis.opsForSet().members(obtainSourceKey());
    }

    @Override
    public boolean removeSourceKey(String sourceKey) {
        //删除资源空间
        redis.opsForSet().remove(obtainSourceKey(), sourceKey);

        //删除指标空间
        redis.opsForHash().entries(obtainIndexDataKey(sourceKey)).keySet().forEach(ip -> redis.opsForHash().delete(obtainIndexDataKey(sourceKey), ip));

        return false;
    }

    @Override
    public boolean removeIp(Set<String> sourceKeys, String ip) {
        //删除探活空间
        redis.opsForHash().delete(obtainHealthKey(), ip);

        //删除指标空间
        sourceKeys.forEach(sourceKey -> redis.opsForHash().delete(obtainIndexDataKey(sourceKey), ip));
        return false;
    }

    @Override
    public List<HealthModel> health(HealthModel healthModel) {
        redis.opsForHash().put(obtainHealthKey(), healthModel.getIp(), JacksonSerializer.serialize(healthModel));
        Map<String, String> healthMap = redis.opsForHash().entries(obtainHealthKey());
        if (CollectionUtils.isEmpty(healthMap)) {
            return new ArrayList<>();
        }
        return healthMap.values()
                .stream()
                .map(val -> JacksonSerializer.deSerialize(val, HealthModel.class))
                .collect(Collectors.toList());
    }

    @Override
    public boolean semaphoreBroadcastOrAck(String tag, String ip, boolean broadcastOrAck) {
        if (broadcastOrAck) {
            //如果没有活跃客户端 不需要广播
            if (liveClientInfo().stream().filter(client->!client.getIp().equals(ip)).count()<1) {
                return true;
            }

            return redis.opsForValue().setIfAbsent(String.format("%s_nx", obtainSemaphoreKey(tag)), "1") ;
        } else {
            redis.opsForSet().add(String.format("%s_queue", obtainSemaphoreKey(tag)), ip);
        }
        return true;
    }

    @Override
    public boolean ifAllAck(String tag) {
        // 考虑机器有可能宕机 ack的量必须大于等于健康检查的值时 才为全部ack
        return redis.opsForSet().size(String.format("%s_queue", obtainSemaphoreKey(tag))) >= liveClientInfo().size();
    }

    /**
     * 获取存活的客户端信息
     * @return
     */
    private List<HealthModel> liveClientInfo() {
         Map<String, String> healthMap = redis.opsForHash().entries(obtainHealthKey());
        if (CollectionUtils.isEmpty(healthMap)) {
            return new ArrayList<>();
        }
        return healthMap.values()
                .stream()
                .map(val -> JacksonSerializer.deSerialize(val, HealthModel.class))
                .collect(Collectors.toList());
    }
    private String obtainIndexDataKey(String sourceKey) {
        return String.format("%s:indexData_%s", obtainIndexDataPath(), sourceKey);
    }

    private String obtainSourceKey() {
        return String.format("%s:sourceKeys", obtainIndexDataPath());
    }

    private String obtainHealthKey() {
        return String.format("%s:health", obtainIndexDataPath());
    }

    private String obtainSemaphoreKey(String tag) {
        return String.format("%s:semaphore_%s", obtainIndexDataPath(), tag);
    }

    private String obtainIndexDataPath() {

        return String.format("%s:%s:rateLimiter", "demo", "prod");
    }

    //redis降级切换 todo
    @Resource(name = "redisTemplate")
    private RedisTemplate redis;
}
