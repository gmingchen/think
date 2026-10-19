package com.dishes.common.utils;

import cn.hutool.json.JSONUtil;
import com.dishes.common.constant.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 好市民
 */
public class Utils {

    /**
     * Obj 数组 转换
     * @param obj
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if(obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
        }
        return result;
    }

    /**
     * 提取字符串
     * @param content 内容
     * @param startMark 开始标记
     * @param endMark 结束标记
     * @return
     */
    public static List<String> extractString(String content, String startMark, String endMark) {
        List<String> result = new ArrayList<>();
        String regex = startMark + "\\w*" + endMark;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            result.add(
                    matcher.group(0)
                            .replaceAll(startMark, "")
                            .replaceAll(endMark, ""));
        }
        return result;
    }

    /**
     * AOP中获取参数 排除 部分类
     * @param args
     * @return
     */
    public static String getParams(Object[] args) {
        if (args != null && args.length > 0) {
            List<Object> list = new ArrayList<>();
            for (Object arg: args) {
                boolean include = Arrays.asList(Constant.EXCLUDE_CLASS).contains(arg.getClass().getName());
                if (!include) {
                    list.add(arg);
                }
            }
            return JSONUtil.toJsonStr(list);
        }
        return null;
    }

    /**
     * 获取string StackTrace
     * @param e 异常
     * @return
     */
    public static String getStringStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = e.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            StackTraceElement element = elements[i];
            sb.append(element.toString());
            if (i < elements.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取进程输出
     * @param process 进程
     */
    public static String getProcessOutput(Process process) throws IOException {
        StringBuilder sb = new StringBuilder();

        InputStream inputStream = process.getInputStream();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                sb.append(line);
            }
        } finally {
            inputStream.close();
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return sb.toString();
    }


}
