package com.dishes.modules.cookreview.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 烹饪评价
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "cook_review", autoResultMap = true)
public class CookReviewEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 烹饪订单ID
     */
    private Long cookOrderId;
    /**
     * 菜谱ID
     */
    private Long dishId;
    /**
     * 评价人用户ID
     */
    private Long userId;
    /**
     * 评分（1-5）
     */
    private Integer rating;
    /**
     * 文字评价
     */
    private String content;
}
