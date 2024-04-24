package com.hmdp.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.EmailDTO;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.MailUtils;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private MailUtils mailUtils;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result checkCode(String email, HttpSession session) {
        //1.判断邮箱格式是否正确
        if (RegexUtils.isEmailInvalid(email)){
            return Result.fail("邮箱格式格式错误，请重新输入！");
        }
        //2.随机生成验证码
        String code = MailUtils.achieveCode();

        //3.将验证码存入Session
//        session.setAttribute("code", code);

        //3 存入redis,使用redis来代替Session技术,并设置两分钟有效期
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + email,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //4.构建短信内容
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setToEmail(email);
        emailDTO.setSubject("龙哥点评APP验证码");
        emailDTO.setContent("尊敬的用户:你好!\n注册验证码为:" + code + "(有效期为两分钟,请勿告知他人)");
        //5.发送短信
//        mailUtils.send(emailDTO);
        log.info("验证码为:{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String email = loginForm.getEmail();
        String code = loginForm.getCode();
        String passWord = loginForm.getPassword();

        //1.判断邮箱格式是否正确
        if (RegexUtils.isEmailInvalid(email)){
            return Result.fail("邮箱格式格式错误，请重新输入！");
        }
        // 2. 从redis中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + email);
//        Object cacheCode = session.getAttribute("code");
        //3.判断验证码是否符合规则且相同
        if (code == null || !cacheCode.equals(code)){
            return Result.fail("验证码错误，请重新输入！");
        }

        //4. 判断邮箱是否存在
        User user = query().eq("email", email).one();
//        User one = lambdaQuery().eq(User::getEmail, email).one();
        //5. 不存在时注册为新用户
        if (Objects.isNull(user)){
          user = saveUserByEmail(email,passWord);
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //6.保存信息到session中
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        // 6.保存登录信息到redis，代替使用session
        //6.1 生成token,作为登录认证
        String token = UUID.randomUUID().toString(true);
        //6.2 将对象转换为map，要使用stringRedisTemplate，map的键值对必须都为字符串
        HashMap<String, String> userMap = new HashMap<>();
        userMap.put("id",user.getId().toString());
        userMap.put("nickName",user.getNickName());
        userMap.put("icon",user.getIcon());
        //6.3 将map存入redis，并设置过期时间
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token,userMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);

        //7.登录成功删除验证码
        stringRedisTemplate.delete(RedisConstants.LOGIN_CODE_KEY + email);
        return Result.ok(token);
    }

    private User saveUserByEmail(String email,String password){
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        //设置默认昵称，固定前缀+默认字符串
        user.setNickName("user_" + RandomUtil.randomString(8));
        save(user);
        return user;
    }
}
