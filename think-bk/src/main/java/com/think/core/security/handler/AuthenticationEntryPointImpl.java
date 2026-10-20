package com.think.core.security.handler;

import com.think.common.enums.ResultCodeEnum;
import com.think.common.pojo.Result;
import com.think.common.utils.ServletUtils;
import com.think.core.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 好市民
 */
@Slf4j
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        Result<ResultCodeEnum> result = null;
        String token = SecurityUtils.getToken(request);

        if (StringUtils.isBlank(token)) {
            result = Result.error(ResultCodeEnum.NOT_LOGIN);
            log.debug("[访问 URL {} 时，未登录]", request.getRequestURL());
        } else {
            result = Result.error(ResultCodeEnum.TOKEN_EXPIRE);
            log.debug("[访问 URL {} 时，凭证已过期]", request.getRequestURL());
        }
        ServletUtils.write(response, result);
    }
}
