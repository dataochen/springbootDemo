import org.egg.rateLimiter.RateLimiterDemo;
import org.junit.Test;
import org.springframework.aop.framework.AopContext;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * User: dt.chen
 * Date: 2023/4/20
 * Time: 13:39
 */
public class RateLimiterDemoTest extends BootBaseTest  {
    @Resource
    RateLimiterDemo rateLimiterDemo;

    @Test
    public void moreThreadRun() throws InterruptedException {
        for (int i = 0; i < 800; i++) {
            washRateExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            rateLimiterDemo.exe();
                            break;
                        } catch (Exception e) {
//                            System.out.println("重试");
                        }
                    }
                }
            });
        }
     Thread.sleep(100000);
//        rateLimiterDemo.exe();

    }

    ThreadPoolExecutor washRateExecutor=new ThreadPoolExecutor(10,10,1000, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>());

}