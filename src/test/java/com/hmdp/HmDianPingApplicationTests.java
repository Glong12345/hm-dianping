package com.hmdp;

import com.hmdp.dto.EmailDTO;
import com.hmdp.utils.MailUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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


}
