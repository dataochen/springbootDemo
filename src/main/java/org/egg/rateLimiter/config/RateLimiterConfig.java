package org.egg.rateLimiter.config;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Description:限速器配置
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 10:53
 */
@Getter
@Setter
public class RateLimiterConfig implements Serializable {
    /**
     * 限速器开关
     */
    private boolean rateLimiterSwitch=Boolean.TRUE;
    /**
     * 动态负载均衡开关
     */
    private boolean loadBalanceSwitch=Boolean.TRUE;

    /**
     * 是否要求qps要严谨的 不允许出现超过qps最大值的情况 默认false
     * TRUE；新机器启动时 会阻塞等待其他机器调低max值后 才能启动成功
     * FALSE：新机器启动不受阻 但会出现短时间内 客户端max累计超过预期qps限制 max
     *
     */
    private boolean exactSwitch=Boolean.FALSE;
    /**
     * 刷新指标数据
     * 间隔时间 单位 秒
     */
    private Integer refreshDataTimeScope=5;

    /**
     *
     * 探活间隔时间 单位 秒
     */
    private Integer healthTimeScope=1;

    /**
     * 探活 最大超时时间 当本地时间和控制台某客户端的上次呼吸时间相差>healthLimitTime时 认定为客户端不可用
     */
    private Integer healthLimitTime=60;

//    /**
//     * 懒加载开关
//     * TRUE：系统启动时不初始化限速器，当有请求时才初始化限速器
//     * FALSE:与上相反
//     */
//    private boolean lazyLoad;
}
