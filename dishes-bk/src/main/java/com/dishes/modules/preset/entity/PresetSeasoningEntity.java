package com.dishes.modules.preset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 预设调料
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "preset_seasoning", autoResultMap = true)
public class PresetSeasoningEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 调料名称
     */
    private String name;
    /**
     * 分类（酱油/醋/酱/油/辛香料/调味料）
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
