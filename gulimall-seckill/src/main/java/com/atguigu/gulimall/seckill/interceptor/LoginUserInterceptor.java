package com.atguigu.gulimall.seckill.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRespVo;
import org.apache.shiro.util.AntPathMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component  // 拦截器
public class LoginUserInterceptor implements HandlerInterceptor {
  public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

  @Override // 前置拦截
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // 解决重复查询拦截
    MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LONGIN_USER);
    if (attribute != null) {
      loginUser.set(attribute);
      // 登录了
      return true;
    } else {
      // 没登录就去登录
      request.getSession().setAttribute("msg", "请先进行登录");
      response.sendRedirect("http://auth.gulimall.com/login.html");
      return false;
    }
  }
}
