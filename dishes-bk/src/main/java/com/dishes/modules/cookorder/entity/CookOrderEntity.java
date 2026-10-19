package com.dishes.modules.cookorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 烹饪订单
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "cook_order", autoResultMap = true)
public class CookOrderEntity extends BasePO {
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
     * 下单人用户ID
     */
    private Long ordererId;
    /**
     * 订单日期
     */
    private LocalDate date;
    /**
     * 时段：1-早餐 2-午餐 3-晚餐 4-宵夜
     */
    private Integer timePeriod;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态：1-已下单 2-已上菜 3-已评价 4-已取消
     */
    private Integer status;
    /**
     * 上菜时间
     */
    private LocalDateTime servedAt;
    /**
     * 评价时间
     */
    private LocalDateTime reviewedAt;
    /**
     * 取消时间
     */
    private LocalDateTime cancelledAt;
}
