package com.think.filter.xss;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS过滤处理
 * @author 好市民
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    HttpServletRequest httpServletRequest;

    private final static HTMLFilter htmlFilter = new HTMLFilter();

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
        httpServletRequest = request;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        String superHeader = super.getHeader(HttpHeaders.CONTENT_TYPE);
        // 非json类型，直接返回
        if(!MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(superHeader) &&
                !(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8).equalsIgnoreCase(superHeader)){
            return super.getInputStream();
        }

        // 为空，直接返回
        String json = IOUtils.toString(super.getInputStream(), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(json)) {
            return super.getInputStream();
        }

        // xss过滤
        json = xssEncode(json);
        final ByteArrayInputStream bis = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return true;
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setReadListener(ReadListener readListener) {

            }
            @Override
            public int read() throws IOException {
                return bis.read();
            }
        };
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(xssEncode(name));
        if (StringUtils.isNotBlank(value)) {
            value = xssEncode(value);
        }
        return value;
    }


    @Override
    public String[] getParameterValues(String name) {
        String[] parameters = super.getParameterValues(name);
        if (parameters == null || parameters.length == 0) {
            return null;
        }
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = xssEncode(parameters[i]);
        }
        return parameters;
    }

    @Override
    public Map<String,String[]> getParameterMap() {
        Map<String,String[]> map = new LinkedHashMap<>();
        Map<String,String[]> parameters = super.getParameterMap();
        for (String key : parameters.keySet()) {
            String[] values = parameters.get(key);
            for (int i = 0; i < values.length; i++) {
                values[i] = xssEncode(values[i]);
            }
            map.put(key, values);
        }
        return map;
    }

    /**
     * 请求头中的 Accept 将不过滤
     * 如果过滤将会将所有的分号过滤掉
     * 抛出 InvalidMediaTypeException: Invalid mime type "application/xmlq=0.9": Invalid token character '=' in token "xmlq=0.9"
     * @param name
     * @return
     */
    @Override
    public String getHeader(String name) {
        String acceptKey = "Accept";
        String value = super.getHeader(xssEncode(name));
        if (!acceptKey.equals(name) && StringUtils.isNotBlank(value)) {
            value = xssEncode(value);
        }
        return value;
    }

    /**
     * 过滤
     * @param input
     * @return
     */
    private String xssEncode(String input) {
        input = htmlFilter.filter(input);
        return input;
    }

    /**
     * 获取最原始的request
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }
}
