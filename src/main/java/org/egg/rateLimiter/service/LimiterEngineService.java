package org.egg.rateLimiter.service;


import org.egg.rateLimiter.mode.EngineTypeEnum;
import org.egg.rateLimiter.mode.HealthModel;
import org.egg.rateLimiter.mode.IndexData;

import java.util.List;
import java.util.Set;

/**
 * Description:
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 11:01
 */
public interface LimiterEngineService {

    /**
     * 控制台处理类key
     *
     * @return
     */
    EngineTypeEnum engine();

    /**
     * 推送当前客户端的指标信息到控制台
     *
     * @param sourceKey
     * @param indexData
     * @return
     */
    boolean pushIndexData(String sourceKey, IndexData indexData);

    /**
     * 从控制台拉取所有客户端的指标信息
     *
     * @return
     */
    List<IndexData> pullIndexDataList(String sourceKey);

    /**
     * 推送当前客户端的资源key信息到控制台
     *
     * @param sourceKey
     * @return
     */
    boolean pushSourceKey(String sourceKey);

    /**
     * 从控制台拉取所有资源key
     *
     * @return
     */
    Set<String> pullSourceKeys();

    /**
     * 指标信息监听
     *
     * @param indexData
     * @return
     */
    default boolean indexDataListener(IndexData indexData) {
        throw new UnsupportedOperationException("暂不支持");
    }

    /**
     * 移除无用sourceKey
     * 当系统迭代后，需要自动校验出客户端和控制台的sourceKey差异 移除无用sourceKey
     *
     * @param sourceKey
     * @return
     */
    boolean removeSourceKey(String sourceKey);

    /**
     * 移除无效ip
     * 当客户端缩容和系统宕机后 需要移除无效ip
     *
     * @param sourceKeys
     * @param ip
     * @return
     */
    boolean removeIp(Set<String> sourceKeys, String ip);

    /**
     * 客户端探活
     * 1.推送本次呼吸时间信息
     * 2.拉取所有客户端的呼吸信息
     *
     * @return
     */
    List<HealthModel> health(HealthModel healthModel);

    /**
     * 分布式信号量 用于广播给客户端
     *
     * @param tag
     * @param broadcastOrAck true:广播 false:ack
     * @return
     */
    boolean semaphoreBroadcastOrAck(String tag,String ip, boolean broadcastOrAck);

    /**
     * 分布式信号量 所有客户端是否已响应
     * @param tag
     * @return
     */
    boolean ifAllAck(String tag);

}
