package com.dishes.filter.stream;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求流转换为多次读取的请求流 过滤器
 * @author 好市民
 */
public class InputStreamFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 转换为可以多次获取流的request
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        InputStreamWrapper inputStreamHttpServletRequestWrapper = new InputStreamWrapper(httpServletRequest);
        // 放行
        chain.doFilter(inputStreamHttpServletRequestWrapper, response);
    }

}
