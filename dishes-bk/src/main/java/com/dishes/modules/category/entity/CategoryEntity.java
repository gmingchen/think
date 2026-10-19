package com.dishes.modules.category.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 分类
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "category", autoResultMap = true)
public class CategoryEntity extends BasePO {
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
     * 分类名称
     */
    private String name;
    /**
     * 排序值
     */
    private Integer sort;
}