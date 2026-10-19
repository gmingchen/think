package com.dishes.common.convert;

import org.springframework.beans.BeanUtils;

/**
 * 基础转换接口
 * @author 好市民
 */
public interface BaseConvert {

    /**
     * 属性拷贝（同名属性自动复制）
     * @param source 源对象
     * @param target 目标对象
     */
    default void copy(Object source, Object target) {
        BeanUtils.copyProperties(source, target);
    }
}
