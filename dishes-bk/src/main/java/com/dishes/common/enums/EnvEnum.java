package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 环境枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum EnvEnum {
    /**
     * 环境
     */
    DEV("dev", "开发环境"),
    PROD("prod", "生产环境"),
    TEST("test", "测试环境"),
    ;

    /**
     * 类型值
     */
    private final String code;
    /**
     * 类型名
     */
    private final String message;

}
