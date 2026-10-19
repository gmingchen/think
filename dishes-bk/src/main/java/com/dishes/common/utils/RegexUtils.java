package com.dishes.common.utils;

import java.util.regex.Pattern;

/**
 * 正则校验工具类
 * @author 好市民
 */
public class RegexUtils {

    /**
     * 用户名（字母开头，包含字母、数字、下划线，4~12位）
     * @param input
     * @return
     */
    public static boolean isUsername(String input) {
        String regex = "[a-zA-Z]\\w{3,11}";
        return Pattern.matches(regex, input);
    }

    /**
     * 手机号（1开头，11位数字）
     * @param input
     * @return
     */
    public static boolean isMobile(String input) {
        String regex = "1\\d{10}";
        return Pattern.matches(regex, input);
    }

    /**
     * 邮箱（字母|数字|下划线 + @ + . + 字母 + [. + 字母]）
     * @param input
     * @return
     */
    public static boolean isEmail(String input) {
        String regex = "\\w+@\\w+\\.[a-z]+(\\.[a-z]+)?";
        return Pattern.matches(regex, input);
    }

    /**
     * 是否是图片类型
     * @param input 文件类型
     * @return
     */
    public static boolean isImage(String input) {
        String regex = "image/[A-Za-z]{2,4}";
        return Pattern.matches(regex, input);
    }
}
