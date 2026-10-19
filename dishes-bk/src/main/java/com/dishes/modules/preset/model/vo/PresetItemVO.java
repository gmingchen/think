package com.dishes.modules.preset.model.vo;

import lombok.Data;

/**
 * 预设项VO
 * @author 好市民
 */
@Data
public class PresetItemVO {
    /**
     * ID
     */
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 分类
     */
    private String category;
    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 是否默认选中
     */
    private Boolean isDefault;
}
