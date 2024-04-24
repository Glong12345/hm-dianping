package com.hmdp.utils;
import cn.hutool.extra.mail.Mail;
import cn.hutool.extra.mail.MailAccount;
import com.hmdp.dto.EmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

@Component
public class MailUtils {
    @Value("${spring.mail.email}")
    private  String email;
    @Value("${spring.mail.host}")
    private  String host;
    @Value("${spring.mail.port}")
    private  String port;
    @Value("${spring.mail.username}")
    private  String username;
    @Value("${spring.mail.password}")
    private  String password;


    // 生成随机验证码
    public static String achieveCode() {  //由于数字 1 、 0 和字母 O 、l 有时分不清楚，所以，没有数字 1 、 0
        String[] beforeShuffle = new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
                "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a",
                "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z"};
        List<String> list = Arrays.asList(beforeShuffle);//将数组转换为集合
        Collections.shuffle(list);  //打乱集合顺序
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s); //将集合转化为字符串
        }
        return sb.substring(3, 8);
    }

    // 利用hutool工具包发送邮件
    public void send(EmailDTO emailDto) {
        // 读取邮箱配置
        if (email == null || host == null || port == null || username == null || password == null) {
            throw new RuntimeException("邮箱配置异常");
        }

        // 设置
        MailAccount account = new MailAccount();
        account.setHost(host);
        account.setPort(Integer.parseInt(port));
        // 设置发送人邮箱
        account.setFrom(username + "<" + email + ">");
        // 设置发送人名称
        account.setUser(username);
        // 设置发送授权码
        account.setPass(password);
        account.setAuth(true);
        // ssl方式发送
        account.setSslEnable(true);
        // 使用安全连接
        account.setStarttlsEnable(true);

        // 发送邮件
        try {
//            int size = emailDto.getTos().size();
            Mail.create(account)
                    //一次只发送给一个目标用户
                    .setTos(emailDto.getToEmail())
//                    .setTos(emailDto.getTos().toArray(new String[size]))
                    .setTitle(emailDto.getSubject())
                    .setContent(emailDto.getContent())
                    .setHtml(true)
                    //关闭session
                    .setUseGlobalSession(false)
                    .send();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

//    public static void sendTestMail(String email, String code) throws MessagingException {
//        // 创建Properties 类用于记录邮箱的一些属性
//        Properties props = new Properties();
//        // 表示SMTP发送邮件，必须进行身份验证
//        props.put("mail.smtp.auth", "true");
//        props.setProperty("mail.transport.protocol", "smtp");   // 使用的协议（JavaMail规范要求）
//        //此处填写SMTP服务器
//        props.put("mail.smtp.host", "smtp.163.com");
//        //端口号，QQ邮箱端口587
//        props.put("mail.smtp.port", "465");
//        // 此处填写，写信人的账号
//        props.put("mail.user", "gl3892702@163.com");
//        // 此处填写16位STMP口令
//        props.put("mail.password", "PDAFZMTCUYHRLKPX");
//        // 构建授权信息，用于进行SMTP进行身份验证
//        Authenticator authenticator = new Authenticator() {
//            protected PasswordAuthentication getPasswordAuthentication() {
//                // 用户名、密码
//                String userName = props.getProperty("mail.user");
//                String password = props.getProperty("mail.password");
//                return new PasswordAuthentication(userName, password);
//            }
//        };
//        // 使用环境属性和授权信息，创建邮件会话
//        Session mailSession = Session.getInstance(props, authenticator);
//        // 创建邮件消息
//        MimeMessage message = new MimeMessage(mailSession);
//        // 设置发件人
//        InternetAddress form = new InternetAddress(props.getProperty("mail.user"));
//        message.setFrom(form);
//        // 设置收件人的邮箱
//        InternetAddress to = new InternetAddress(email);
//        message.setRecipient(RecipientType.TO, to);
//        // 设置邮件标题
//        message.setSubject("邮件登录测试");
//        // 设置邮件的内容体
//        message.setContent("尊敬的用户:你好!\n注册验证码为:" + code + "(有效期为一分钟,请勿告知他人)", "text/html;charset=UTF-8");
//        // 最后当然就是发送邮件啦
//        Transport.send(message);
//    }
}