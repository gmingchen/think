package com.dishes.filter.stream;

import org.apache.commons.io.IOUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

/**
 * 处理多次获取请求流数据会为空的问题
 * 拷贝一份
 * @author 好市民
 */
public class InputStreamWrapper extends HttpServletRequestWrapper {

    /**
     * 用于缓存输入流
     */
    private ByteArrayOutputStream cachedBytes;

    public InputStreamWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBytes == null) {
            // 首次获取流时，将流放入 缓存输入流 中
            cacheInputStream();
        }

        // 从 缓存输入流 中获取流并返回
        return new CachedServletInputStream(cachedBytes.toByteArray());
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    /**
     * 首次获取流时，将流放入 缓存输入流 中
     */
    private void cacheInputStream() throws IOException {
        // 缓存输入流以便多次读取。为了方便, 我使用 org.apache.commons IOUtils
        cachedBytes = new ByteArrayOutputStream();
        IOUtils.copy(super.getInputStream(), cachedBytes);
    }

    /**
     * 读取缓存的请求正文的输入流
     * <p>
     * 用于根据 缓存输入流 创建一个可返回的
     */
    public static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream input;

        public CachedServletInputStream(byte[] buf) {
            // 从缓存的请求正文创建一个新的输入流
            input = new ByteArrayInputStream(buf);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener listener) {

        }

        @Override
        public int read() throws IOException {
            return input.read();
        }
    }

}
