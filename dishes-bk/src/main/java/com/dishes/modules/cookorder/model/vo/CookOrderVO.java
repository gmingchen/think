package com.dishes.modules.cookorder.model.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 烹饪订单列表VO
 * @author 好市民
 */
@Data
public class CookOrderVO {
    /**
     * 订单ID
     */
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
     * 下单人昵称
     */
    private String ordererName;
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
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 评价评分（已评价时返回平均评分）
     */
    private Double rating;
    /**
     * 菜品列表
     */
    private List<CookOrderDishVO> dishes;
}
