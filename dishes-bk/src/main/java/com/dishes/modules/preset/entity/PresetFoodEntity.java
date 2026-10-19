package com.dishes.modules.preset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 预设食材
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "preset_food", autoResultMap = true)
public class PresetFoodEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 食材名称
     */
    private String name;
    /**
     * 分类（蔬菜/肉类/水产/蛋类/菌菇/豆制品/主食）
     */
    private String category;
    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 是否默认选中：0-否 1-是
     */
    private Boolean isDefault;
}
