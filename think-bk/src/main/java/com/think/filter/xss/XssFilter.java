package com.think.filter.xss;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * XssFilter配置
 * @author 好市民
 */
public class XssFilter implements Filter {

    /**
     * 不做 XSS 过滤的路径后缀。
     * 微信消息推送的报文是机器对机器的传输体，本系统永远不会把它当 HTML 渲染，过滤在这里零收益；
     * 但 HTMLFilter 会真实篡改报文内容（& 转成 &amp;、剥掉尖括号标签、给不平衡的尖括号补闭合标签并拼到报文尾部），
     * 而推送的 signature 只覆盖 token/timestamp/nonce、不覆盖 body，改坏了验不出来，
     * 后果是解析失败回 ErrCode -1、微信重推 15 次后放弃，用户付了钱却发不出货。
     */
    private static final String[] EXCLUDE_SUFFIXES = {"/wechat/notify"};

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (isExcluded(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(
                httpRequest);
        chain.doFilter(xssRequest, response);
    }

    /**
     * 用 endsWith 而不是 equals：requestURI 带着 context-path（实际是 /think/wechat/notify），
     * 换 context-path 时不该把排除规则一起改掉
     */
    private boolean isExcluded(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        for (String suffix : EXCLUDE_SUFFIXES) {
            if (uri.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void destroy() {
    }
}
