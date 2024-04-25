package com.hmdp;

import com.hmdp.dto.EmailDTO;
import com.hmdp.utils.MailUtils;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private MailUtils mailUtils;
    @Test
    public void testEmail(){
        EmailDTO emailDTO = new EmailDTO();
//        List<String> list = new ArrayList<>();
//        list.add("1160147044@qq.com");
//        emailDTO.setTos(list);
        emailDTO.setToEmail("1160147044@qq.com");
        emailDTO.setSubject("龙哥点评APP验证码");
        emailDTO.setContent("尊敬的用户:你好!\n注册验证码为:" + MailUtils.achieveCode() + "(有效期为一分钟,请勿告知他人)");
        System.out.println("666");
        mailUtils.send(emailDTO);
        System.out.println("test");
    }

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);
    @Test
    void testIdWork() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id= "+ id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time= "+ (end - begin));
    }


}
