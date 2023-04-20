package org.egg.rateLimiter.mode;

import lombok.*;

import java.io.Serializable;

/**
 * Description:探活实体
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 15:56
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HealthModel implements Serializable {
    private String ip;
    private Long lastBreatheTime;

}
