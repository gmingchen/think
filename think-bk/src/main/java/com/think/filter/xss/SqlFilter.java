package com.think.filter.xss;

import com.think.common.enums.ResultCodeEnum;
import com.think.exception.RunException;
import org.apache.commons.lang3.StringUtils;

/**
 * SQL过滤
 * @author 好市民
 */
public final class SqlFilter {
    /**
     * SQL注入过滤
     * @param str 待验证的字符串
     */
    public String filter(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        // 去掉'|"|;|\字符
        str = StringUtils.replace(str, "'", "");
        str = StringUtils.replace(str, "\"", "");
        str = StringUtils.replace(str, ";", "");
        str = StringUtils.replace(str, "\\", "");
        // 转换成小写
        str = str.toLowerCase();
        // 非法字符
        String[] keywords = { "master", "truncate", "insert", "select", "delete", "update", "declare", "alert",
                "create", "drop" };
        // 判断是否包含非法字符
        for (String keyword : keywords) {
            if (str.equals(keyword)) {
                throw new RunException(ResultCodeEnum.BAD_SQL);
            }
        }

        return str;
    }
}
