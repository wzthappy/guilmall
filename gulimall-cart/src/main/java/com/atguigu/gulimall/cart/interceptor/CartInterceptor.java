package com.atguigu.gulimall.cart.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行目标方法之前，判断用户的登录状态。并封装传递给controller目标请求。
 */
public class CartInterceptor implements HandlerInterceptor {  // 拦截器
  public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>(); // 线程私有(每个线程都有一个)


  /**
   * 目标方法执行之前
   */
  @Override
  public boolean preHandle(HttpServletRequest request,
                           HttpServletResponse response, Object handler) throws Exception {
    UserInfoTo userInfoTo = new UserInfoTo();
    HttpSession session = request.getSession();
    MemberRespVo member = (MemberRespVo) session.getAttribute(AuthServerConstant.LONGIN_USER);
    if (member != null) {
      // 用户登录了
      userInfoTo.setUserId(member.getId());
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null && cookies.length > 0) {
      for (Cookie cookie : cookies) {
        // user-key
        String name = cookie.getName();
        if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
          userInfoTo.setUserKey(cookie.getValue());
          userInfoTo.setTempUser(true);
        }
      }
    }

    // 如果没有临时用户，浏览器一定要分配一个临时用户
    if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
      String uuid = UUID.randomUUID().toString();
      userInfoTo.setUserKey(uuid);
    }

    // 目标方法执行之前
    threadLocal.set(userInfoTo);
    return true;
  }

  /**
   * 业务执行之后
   */
  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    UserInfoTo userInfoTo = threadLocal.get();
    // 如果cookie中没有临时用户，就把这个临时用会保存起来
    if (!userInfoTo.isTempUser()) { // 如果有userKey就不需要添加对应的cookie了
      Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
      cookie.setDomain("gulimall.com"); // 作用域
      cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
      response.addCookie(cookie);
    }
  }
}
