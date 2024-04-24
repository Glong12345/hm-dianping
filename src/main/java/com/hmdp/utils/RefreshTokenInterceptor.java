package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断用户是否登录
//         1. 获取请求头中的session
//        HttpSession session = request.getSession();
//        // 2. 判断session中是否有user
//        Object user = session.getAttribute("user");
//        // 3. 判断user是否为空
//        if (Objects.isNull(user)){
//            response.setStatus(401);
//            return false;
//        }

        // 使用redis来判断用户是否登录
        //1. 获取请求头中的token
        String token = request.getHeader("authorization");

        //2. 判断token是否为空
        if (StrUtil.isBlank(token)){
            // 为空直接放行，交给登录拦截器处理，这里只做token刷新有效期
            return true;
        }

        //3. 获取用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        //4. 用户存在，刷新有效期
        if (userMap.size() > 0 ){
            stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

            // 5. 保存到ThreadLocal
            UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
            UserHolder.saveUser(userDTO);
        }

        // 5. 返回true
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 移除ThreadLocal中的用户
        UserHolder.removeUser();
    }
}
