package com.think.config;

import com.think.filter.stream.InputStreamFilter;
import com.think.filter.xss.XssFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * 过滤器配置
 * @author 好市民
 */
@Configuration
public class FilterConfig {

    /**
     * 拷贝请求流 过滤器
     * @return
     */
    @Bean
    public FilterRegistrationBean inputStreamHttpServletRequestFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        // 设置系统过滤器 (setFilter就是你所定义的过滤器filter类)
        registration.setFilter(new InputStreamFilter());
        // 过滤所有路径
        registration.addUrlPatterns("/*");
        // 过滤器名称
        registration.setName("InputStreamFilter");
        // 优先级
        registration.setOrder(HIGHEST_PRECEDENCE + 1);

        return registration;
    }

    /**
     * xss 过滤
     * @return
     */
    @Bean
    public FilterRegistrationBean xssFilterRegister() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        // 设置系统过滤器 (setFilter就是你所定义的过滤器filter类)
        registration.setFilter(new XssFilter());
        // 过滤所有路径
        registration.addUrlPatterns("/*");
        // 过滤器名称
        registration.setName("XssFilter");
        // 优先级
        registration.setOrder(Integer.MAX_VALUE);
        return registration;
    }
}
