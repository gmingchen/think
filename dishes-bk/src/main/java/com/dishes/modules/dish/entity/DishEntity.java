package com.dishes.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 菜谱
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "dish", autoResultMap = true)
public class DishEntity extends BasePO {
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
     * 分类ID
     */
    private Long categoryId;
    /**
     * 创建者用户ID
     */
    private Long creatorId;
    /**
     * 菜名
     */
    private String name;
    /**
     * 封面图URL
     */
    private String cover;
    /**
     * 详情图URL列表（JSON 数组，最多 3 张；JacksonTypeHandler 序列化）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;
    /**
     * 备注
     */
    private String remark;
    /**
     * 做过次数
     */
    private Integer cookCount;
    /**
     * 平均评分（1.0-5.0）
     */
    private java.math.BigDecimal rating;
    /**
     * 被收藏次数
     */
    private Integer collectCount;
    /**
     * 排序值（同厨房同分类内手动升序，从 0 起；新建/改分类取该分类末尾 max+1）
     */
    private Integer sort;
}