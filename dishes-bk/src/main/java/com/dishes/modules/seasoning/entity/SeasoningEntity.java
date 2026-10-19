package com.dishes.modules.seasoning.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 调料
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "seasoning", autoResultMap = true)
public class SeasoningEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 创建者用户ID
     */
    private Long userId;
    /**
     * 调料名称
     */
    private String name;
    /**
     * 使用次数
     */
    private Integer usageCount;
}