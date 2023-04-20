package org.egg.rateLimiter.mode;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Description:指标信息
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 11:02
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class IndexData implements Serializable {
    /**
     * 当前机器的最大QPS指标
     */
    public BigDecimal limit;
    /**
     * 当前机器的最大QPS指标
     */
    public Integer max;
    /**
     * 当前机器的平均QPS指标
     */
    public BigDecimal avg;
    /**
     * 当前机器的IP或唯一标识
     */
    public String ip;
}
