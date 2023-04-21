package org.egg.rateLimiter.client;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.egg.rateLimiter.config.RateLimiterConfig;
import org.egg.rateLimiter.mode.HealthModel;
import org.egg.rateLimiter.mode.IndexData;
import org.egg.rateLimiter.mode.SourceConfig;
import org.egg.rateLimiter.service.LimiterEngineService;
import org.egg.rateLimiter.spring.AnnotationWrapBean;
import org.egg.utils.IpUtil;
import org.egg.utils.ParamChecker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Description:
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 11:57
 */
@Slf4j
public class RateLimiterClient implements InitializingBean {
    private RateLimiterConfig rateLimiterConfig;

    /**
     * 本地限速器
     * key：资源
     * value：限速器
     */
    private Map<String, RateLimiter> limiterMap = new HashMap<>();

    /**
     * 本地计数器
     * * key：资源
     * * value：计数器
     */
    private Map<String, QpsCounter> qpsCounterMap = new HashMap<>();

    private String localIp;


    /**
     * 系统启动时加载
     */
    public void load() {
        ParamChecker.notNull(rateLimiterConfig, "限速器配置未初始化");
        ParamChecker.notNull(rateLimiterConfig.getHealthTimeScope(), "探活间隔时间不能为空");
        ParamChecker.notNull(rateLimiterConfig.getHealthLimitTime(), "探活最大超时时间不能为空");
        ParamChecker.notNull(rateLimiterConfig.getRefreshDataTimeScope(), "刷新指标数据不能为空");

        //加载本地限速器
        ParamChecker.isTrue(CollectionUtils.isEmpty(limiterMap), "重复加载限速器，请检查配置是否重复");
        if (CollectionUtils.isEmpty(AnnotationWrapBean.getLocalRateLimiterDataMap())) {
            log.info("本应用无限速器资源配置。");
            return;
        }
        //todo 懒加载支持

        //获取本机IP
        localIp = IpUtil.getWholeIp();
        ParamChecker.notNull(localIp, "获取本地IP失败，无法启动，请研发检查原因");

        //初始化本地计数器
        preExpansionCounter();

        //初始化本地限速器指标
        rateLimiterRefresh(true);

        //启动定时任务 定时刷新 RateLimiterClientFixedDelay
        ScheduledThreadPoolExecutor loadConfExecutor = new ScheduledThreadPoolExecutor(3);

        //客户端探活
        loadConfExecutor.scheduleWithFixedDelay(this::healthCheck,
                rateLimiterConfig.getHealthTimeScope(), rateLimiterConfig.getHealthTimeScope(), TimeUnit.SECONDS);

        //刷新 计数器
        loadConfExecutor.scheduleWithFixedDelay(this::preExpansionCounter, 1, 1, TimeUnit.SECONDS);

        //刷新本地限速器指标
        loadConfExecutor.scheduleWithFixedDelay(this::rateLimiterRefresh, rateLimiterConfig.getRefreshDataTimeScope(), rateLimiterConfig.getRefreshDataTimeScope(), TimeUnit.SECONDS);
    }


    /**
     * 判断是否需要限流
     *
     * @param sourceKey
     * @return
     */
    public boolean canDo(String sourceKey) {
        RateLimiter rateLimiter = limiterMap.get(sourceKey);
        ParamChecker.notNull(rateLimiter, String.format("找不到资源%s 对应的限速器，代码有问题，需要研发介入", sourceKey));
        boolean res = rateLimiter.tryAcquire();
        if (res) {
            qpsCounterMap.get(sourceKey).addAndGet();
        }
        return res;
    }

    /**
     * 刷新
     */
    private void rateLimiterRefresh() {
        rateLimiterRefresh(false);
    }

    /**
     * 刷新资源和指标
     * todo 降级 如果redis不可用或者报错 直接修改本地限速器为预置值
     *
     * @param firstStart TRUE代表首次刷新（初始化）
     */
    private void rateLimiterRefresh(boolean firstStart) {

        boolean redisOffline = true;

        //本地限速器资源列表
        Map<String, SourceConfig> localRateLimiterDataMap = AnnotationWrapBean.getLocalRateLimiterDataMap();

        if (!redisOffline) {
            //刷新控制台的资源列表
            Set<String> serverRateLimiterSourceKeys = limiterEngineService.pullSourceKeys();
            Sets.difference(localRateLimiterDataMap.keySet(), serverRateLimiterSourceKeys)
                    .forEach(item -> {
                        if (serverRateLimiterSourceKeys.contains(item)) {
                            //需要删除的
                            limiterEngineService.removeSourceKey(item);
                        } else {
                            //需要新增的
                            limiterEngineService.pushSourceKey(item);
                        }
                    });
        }

        //刷新本地和控制台的指标信息
        localRateLimiterDataMap.forEach((key, value) -> {
            QpsCounter qpsCounter = qpsCounterMap.get(key);
            IndexData indexData = IndexData.builder()
                    .limit(redisOffline ? new BigDecimal(value.getLocalLimit()) : calcMax(key, value, qpsCounter.getAvg(), firstStart))
                    .ip(localIp)
                    .max(qpsCounter.getMax())
                    .avg(qpsCounter.getAvg())
                    .build();


            //本地初始化限速器
            changeLimiter(key, indexData.getLimit());

            //推送到控制台指标信息
            if (!redisOffline) {
                limiterEngineService.pushIndexData(key, indexData);
            }
        });

        if (!redisOffline) {
            if (firstStart) {
                //   新机器 推送标识到控制台；同时扩容多台机器时 一台一台的调整客户端max 当前客户端阻塞
                waitTime(() -> limiterEngineService.semaphoreBroadcastOrAck(NEW_CLIENT_FLAG, localIp, true));

                //阻塞 等待其他机器max刷新 才能启动成功
                waitTime(() -> !rateLimiterConfig.isExactSwitch() || limiterEngineService.ifAllAck(NEW_CLIENT_FLAG));
            } else {
                //注意：客户端重复ack问题
                limiterEngineService.semaphoreBroadcastOrAck(NEW_CLIENT_FLAG, localIp, false);
            }
        }
    }

    /**
     * 心跳检查
     * 删除无效ip
     */
    private void healthCheck() {
        long now = System.currentTimeMillis();
        List<HealthModel> healthList = limiterEngineService.health(HealthModel.builder()
                .ip(localIp)
                .lastBreatheTime(now)
                .build());
        Set<String> unLiveIps = healthList.stream()
                .filter(healthModel -> healthModel.getLastBreatheTime() - now > rateLimiterConfig.getHealthLimitTime())
                .map(HealthModel::getIp)
                .collect(Collectors.toSet());
        //删除失活ip
        unLiveIps.forEach(ip -> limiterEngineService.removeIp(limiterEngineService.pullSourceKeys(), ip));
    }

    private void preExpansionCounter() {
        //本地限速器资源列表
        Map<String, SourceConfig> localRateLimiterDataMap = AnnotationWrapBean.getLocalRateLimiterDataMap();

        for (String sourceKey : localRateLimiterDataMap.keySet()) {
            QpsCounter qpsCounter = qpsCounterMap.get(sourceKey);
            if (Objects.isNull(qpsCounter)) {
                qpsCounter = QpsCounter.getInstance();
                qpsCounterMap.put(sourceKey, qpsCounter);
            }
            qpsCounter.preExpansionAndCount();
        }
    }

    private void changeLimiter(String sourceKey, BigDecimal configRate) {
        RateLimiter rateLimiter = limiterMap.get(sourceKey);
        if (Objects.isNull(rateLimiter)) {
            rateLimiter = RateLimiter.create(configRate.doubleValue());
            limiterMap.put(sourceKey, rateLimiter);
        } else {
            if (!Objects.equals(configRate, rateLimiter.getRate())) {
                rateLimiter.setRate(configRate.doubleValue());
            }
        }
    }

    private BigDecimal calcMax(String sourceKey, SourceConfig sourceConfig, BigDecimal avg, boolean firstStart) {
        List<IndexData> indexDataList = limiterEngineService.pullIndexDataList(sourceKey);
        BigDecimal res;
        if (!firstStart) {
            //如果有新机器广播标识
            String newClientIp = limiterEngineService.obtainTag(NEW_CLIENT_FLAG);
            Integer oldMax = indexDataList.stream().filter(indexData -> indexData.getIp().equals(localIp)).findFirst().get().getMax();
            if (Objects.nonNull(newClientIp)) {
                //公式 A_max=A_max-(1/（已有机器数量+1）)*A_max)
                res = new BigDecimal(oldMax).subtract(new BigDecimal(oldMax).divide(new BigDecimal(indexDataList.size()), RoundingMode.HALF_UP));
                log.info("限速器负载均衡引擎 识别到有新机器ip:{} 发布 本客户端old:{}->new:{}", newClientIp, oldMax, res);

            } else {
//            A_max=m*(A_avg/(A_avg+B_avg+...))
                BigDecimal sum = indexDataList.stream().filter(indexData -> !indexData.getIp().equals(localIp))
                        .map(IndexData::getAvg)
                        .reduce(BigDecimal::add)
                        .get();
                res = new BigDecimal(sourceConfig.getMax()).multiply(avg.divide(sum.add(avg), RoundingMode.HALF_UP));
                log.debug("限速器负载均衡引擎 动态刷新 本客户端old:{}->new:{}", oldMax, res);
            }
        } else {
//            ：新机器C_max=(1/（已有机器数量+1）)*A_max+(1/（已有机器数量+1）)*B_max+...
            res = indexDataList.stream()
                    .filter(indexData -> !indexData.getIp().equals(localIp))
                    .map(IndexData::getMax)
                    .map(max -> new BigDecimal(max).divide(new BigDecimal(indexDataList.size() + 1), RoundingMode.HALF_UP))
                    .reduce(BigDecimal::add).orElse(new BigDecimal(sourceConfig.getMax()));
            log.info("限速器负载均衡引擎 新机器发布ip:{},new:{}", localIp, res);

        }
        return res;

    }

    private void waitTime(Supplier<Boolean> supplier) {
        while (!supplier.get()) {
            log.info("限速器启动阻塞 等待其他机器max刷新 才能启动成功");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new IllegalStateException("限速器启动阻塞 等待其他机器max刷新 异常，需要研发介入排查原因！");
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        load();
    }

    public void setRateLimiterConfig(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    private final String NEW_CLIENT_FLAG = "newClient";

    @Resource
    private LimiterEngineService limiterEngineService;
}
