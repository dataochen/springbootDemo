package org.egg.rateLimiter.mode;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Description:
 * User: dt.chen
 * Date: 2023/4/19
 * Time: 17:34
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SourceConfig implements Serializable {
    private int max;
    private int localLimit;
    private String sourceKey;
}
