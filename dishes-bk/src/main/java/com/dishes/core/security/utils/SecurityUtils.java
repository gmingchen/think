package com.dishes.core.security.utils;

import com.dishes.common.constant.Constant;
import com.dishes.common.utils.HttpContextUtils;
import com.dishes.modules.auth.model.LoginUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

/**
 * 安全服务工具
 * @author 好市民
 */
public class SecurityUtils {

    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader(Constant.TOKEN_KEY);
        if (StringUtils.isBlank(token)) {
            token = request.getParameter(Constant.TOKEN_KEY);
        }
        return token;
    }

    public static String getToken() {
        return getToken(HttpContextUtils.getHttpServletRequest());
    }

    public static Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }
        return context.getAuthentication();
    }

    /**
     * 获取当前登录用户
     * @return
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof LoginUser ? (LoginUser) authentication.getPrincipal() : null;
    }

    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    public static void setLoginUser(LoginUser loginUser, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                loginUser, null, null);
        usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
    }

}
