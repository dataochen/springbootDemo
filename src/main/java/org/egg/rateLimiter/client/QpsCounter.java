package org.egg.rateLimiter.client;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:高效的qps计数器
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 17:47
 */
public class QpsCounter {
    private QpsCounter() {

    }

    public static QpsCounter getInstance() {
        return new QpsCounter();
    }

    /**
     * 计数器 只保留120秒的数据
     * key：时间戳 单位 秒
     * value 计数器
     */
    private Map<String, AtomicInteger> counter = new LinkedHashMap<String, AtomicInteger>(120, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AtomicInteger> eldest) {
            return this.size() > 120;
        }
    };
    /**
     * 最大的key 时间戳
     */
    private Long maxKey=0L;

    /**
     * 最大指标
     */
    private Integer max=0;

    /**
     * 计数
     *
     * @return 当前数量
     */
    public int addAndGet() {
        long now = System.currentTimeMillis()/1000;
        return counter.get(now + "").incrementAndGet();
    }

    /**
     * 剩余key量5秒内够用 不扩容,每次扩10s
     * 1.提前扩容 10秒后的计数器
     */
    public void preExpansionAndCount() {
        long now = System.currentTimeMillis()/1000;
        if (maxKey > now +  5) {
            return;
        }

        for (int i = 0; i < 10; i++) {
            counter.put(now + "", new AtomicInteger(0));
            now += 1;
        }
        maxKey = now - 1;
    }

    public Integer getMax() {
        Integer count = counter.values().stream().map(AtomicInteger::get).max(Integer::compareTo).get();
        max = Math.max(max, count);
        return max;
    }

    public BigDecimal getAvg() {
        Integer sum = counter.values().stream().map(AtomicInteger::get).reduce(Integer::sum).get();
        return new BigDecimal((sum*1.0/counter.values().size())+"");
    }
}
