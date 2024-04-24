package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Objects;

// 登录拦截器
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 登录拦截请求
        //1. 获取登录信息
        UserDTO user = UserHolder.getUser();

        //2. 判断是否登录
        if (Objects.isNull(user)){
            //3. 未登录，返回401
            response.setStatus(404);
            return false;
        }

        // 4. 返回true
        return true;
    }

}
