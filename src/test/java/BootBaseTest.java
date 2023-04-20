import org.egg.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * User: yupei.wang
 * Date: 2017/12/8
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@EnableAspectJAutoProxy(exposeProxy = true)
public class BootBaseTest {
    private static final Logger logger = LoggerFactory.getLogger(BootBaseTest.class);

    @Test
    public void test() {
        logger.info("测试类基类正常");
    }
}
