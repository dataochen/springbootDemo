package org.egg.rateLimiter;

import org.egg.rateLimiter.annotation.RateLimiter;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:
 * User: dt.chen
 * Date: 2023/4/20
 * Time: 11:40
 */
@Component
public class RateLimiterDemo {
    private AtomicInteger count = new AtomicInteger(0);

    @RateLimiter(sourceKey = "demo", max = 10)
    public void exe() {
        System.out.println(String.format("%s:执行逻辑:" + count.incrementAndGet(),System.currentTimeMillis()/1000));
    }


    public void moreThreadRun() {
        washRateExecutor.execute(new Runnable() {
            @Override
            public void run() {
                RateLimiterDemo rateLimiterDemo = (RateLimiterDemo) AopContext.currentProxy();
                rateLimiterDemo.exe();
            }
        });
    }

    ThreadPoolExecutor washRateExecutor=new ThreadPoolExecutor(10,10,1000, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>());

}
