package com.dishes.common.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.json.JSONUtil;
import com.dishes.common.enums.MediaTypeEnum;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;

/**
 * Servlet 工具类
 * @author 好市民
 */
public class ServletUtils {

    /**
     * 返回 JSON 字符串
     * @param response 响应
     * @param object 对象，会序列化成 JSON 字符串
     */
    public static void write(HttpServletResponse response, Object object) {
        String content = JSONUtil.toJsonStr(object);
        ServletUtil.write(response, content, MediaTypeEnum.JSON_UTF8.getValue());
    }

    /**
     * 返回 文件
     *
     * @param response 响应
     * @param filename 文件名
     * @param content 附件内容
     * @throws IOException
     */
    public static void write(HttpServletResponse response, String filename, byte[] content) throws IOException {
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        IoUtil.write(response.getOutputStream(), false, content);
    }

    /**
     * 返回 文件
     *
     * @param response 响应
     * @param content 附件内容
     * @param contentType 内容类型
     * @throws IOException
     */
    public static void write(HttpServletResponse response, byte[] content, String contentType) throws IOException {
        response.setContentType(contentType);
        IoUtil.write(response.getOutputStream(), false, content);
    }
}
