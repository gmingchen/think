package com.think.common.constant;

/**
 * 常量
 * @author 好市民
 */
public class Constant {
    /**
     * 是否使用redis
     */
    public static final boolean USE_REDIS = true;
    /**
     * token 键值
     */
    public static final String TOKEN_KEY = "token";
    /**
     * 注解 内获取参数 需要排除的类型 主要是 HttpServletResponse HttpServletRequest 对象
     */
    public static final String[] EXCLUDE_CLASS = {
            "org.apache.catalina.connector.ResponseFacade",
            "com.think.filter.xss.XssHttpServletRequestWrapper"
    };
    /**
     * 日期格式
     */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String MONTH_DAY_PATTERN = "MM月dd日";
    public static final String DATETIME_NUMBER_PATTERN = "yyyyMMddHHmmss";
    /**
     * 用户主键 key
     */
    public static final String USER_KEY = "userId";
}
