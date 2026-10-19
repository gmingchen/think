package com.dishes.modules.preset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 预设分类
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "preset_category", autoResultMap = true)
public class PresetCategoryEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 是否默认选中：0-否 1-是
     */
    private Boolean isDefault;
}
