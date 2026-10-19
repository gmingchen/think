package com.dishes.common.utils;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP上下文 工具类
 * @author 好市民
 */
public class HttpContextUtils {
    /**
     * 获取 HttpServletRequest
     * @return
     */
    public static HttpServletRequest getHttpServletRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(requestAttributes -> (ServletRequestAttributes)requestAttributes)
                .map(servletRequestAttributes -> servletRequestAttributes.getRequest())
                .orElse(null);
    }

    /**
     * 获取请求头内容
     * @param key 请求头键值
     * @return
     */
    public static String getHeader(String key) {
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> request.getHeader(key))
                .orElse(null);
    }

    /**
     * 获取IP
     * @return
     */
    public static String getIp() {
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> {
                    String ip = request.getHeader("X-Forwarded-For");
                    if(StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)){
                        // 多次反向代理后会有多个ip值，第一个ip才是真实ip
                        int index = ip.indexOf(",");
                        if(index != -1){
                            return ip.substring(0,index);
                        }else{
                            return ip;
                        }
                    }
                    ip = request.getHeader("X-Real-IP");
                    if(StringUtils.isNotEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)){
                        return ip;
                    }
                    return request.getRemoteAddr();
                }).orElse(null);
    }

    /**
     * 获取域名
     * @return
     */
    public static String getDomain() {
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> {
                    StringBuffer url = request.getRequestURL();
                    return url.delete(url.length() - request.getRequestURI().length(), url.length()).toString();
                }).orElse(null);
    }

    /**
     * 获取用户代理
     * @return
     */
    public static String getUserAgent(){
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> request.getHeader("User-Agent"))
                .orElse(null);
    }

    /**
     * 获取浏览器名称
     * @return
     */
    public static String getBrowser() {
        return Optional.ofNullable(getUserAgent())
                .map(userAgent -> {
                    UserAgent ua = UserAgentUtil.parse(userAgent);
                    return ua.getBrowser().toString();
                }).orElse(null);
    }

    /**
     * 获取操作系统
     * @return
     */
    public static String getOperatingSystem() {
        return Optional.ofNullable(getUserAgent())
                .map(userAgent -> {
                    UserAgent ua = UserAgentUtil.parse(userAgent);
                    return ua.getOs().toString();
                }).orElse(null);
    }

    /**
     * 获取所有请求头
     * @return
     */
    public static Map<String, String> getHeaders() {
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> {
                    Map<String, String> headers = new HashMap<>();
                    Enumeration<String> headerNames = request.getHeaderNames();
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        String value = request.getHeader(name);
                        headers.put(name, value);
                    }
                    return headers;
                }).orElse(null);
    }

    /**
     * 获取URL拼接参数
     * @return
     */
    public static Map<String, Object> getUrlParams() {
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> {
                    Map<String, Object> map = new HashMap<>(request.getParameterMap().size());
                    request.getParameterMap().forEach((key, values) -> {
                        Object value = values.length == 1 ? values[0] : values;
                        map.put(key, value);
                    });
                    return map;
                }).orElse(null);
    }
    /**
     * 获取Body参数
     * @return
     */
    public static Map<Object, Object> getBodyParams() {
        return Optional.ofNullable(getHttpServletRequest())
                .map(request -> {
                    Map<Object,Object> params = new HashMap<>(0);
                    BufferedReader bufferedReader = null;
                    try {
                        bufferedReader = request.getReader();
                        String temp;
                        StringBuilder stringParams = new StringBuilder();
                        while((temp = bufferedReader.readLine()) != null){
                            stringParams.append(temp);
                        }
                        if(StringUtils.isNotBlank(stringParams.toString())){
                            params = JSONUtil.toBean(stringParams.toString(), Map.class);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return params;
                }).orElse(null);
    }

    /**
     * 获取请求URL
     * @return
     */
    public static String getRequestUrl(){
        return Optional.ofNullable(getHttpServletRequest())
                .map(HttpServletRequest::getRequestURI)
                .orElse(null);
    }
}
