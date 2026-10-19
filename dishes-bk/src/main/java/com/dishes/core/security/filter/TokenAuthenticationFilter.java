package com.dishes.core.security.filter;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.modules.auth.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.beans.factory.annotation.Autowired;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Token 过滤器 验证token
 * 验证通过 将用户信息存入 Security 上下文
 * @author 好市民
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = SecurityUtils.getToken(request);

        if (StringUtils.isNotBlank(token) && authService.validateToken(token)) {
            Optional.ofNullable(authService.queryUser(token))
                    .ifPresent(loginUser -> SecurityUtils.setLoginUser(loginUser, request));
        }

        filterChain.doFilter(request, response);
    }
}
