package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum DataSourceEnum {
    /**
     * 默认数据源-主数据源
     */
    MASTER("master"),
    /**
     * 数据源
     */
    SLAVE("slave"),
    ;

    private final String value;

}
