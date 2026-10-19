package com.dishes.modules.kitchen.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 厨房VO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenVO {
    /**
     * ID
     */
    private Long id;
    /**
     * 厨房名称
     */
    private String name;
    /**
     * 厨房图标
     */
    private String cover;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 当前用户在该厨房的角色
     */
    private Integer role;
    /**
     * 最大分类数
     */
    private Integer maxCategoryCount;
    /**
     * 最大菜谱数
     */
    private Integer maxDishCount;
    /**
     * 最大成员数
     */
    private Integer maxMemberCount;
    /**
     * 成员数
     */
    private Integer memberCount;
    /**
     * 菜谱数
     */
    private Integer dishCount;
    /**
     * 分类数
     */
    private Integer categoryCount;
    /**
     * 当前登录人是否为该厨房的厨师长（创建者），v12 只读标记、不落库（docs/v12 §6.2）
     * <p>
     * 前端用它的理由不是“显示身份”，而是“无限容量与会员引导行只属于 owner”（§1.4 决策 1）。
     * </p>
     */
    private Boolean isOwner;
    /**
     * 当前登录人在该厨房里是否不受分类与菜谱上限约束，v12 只读标记、不落库
     * <p>
     * = 畅厨会员在期 ∧ {@code isOwner}。为 true 时前端容量条画「x/不限」并隐藏 📺 扩容入口。
     * 后端不用假上限（如 999999）表达不限，否则百分比与剩余量都要特判，所以单独给一个布尔位。
     * </p>
     */
    private Boolean unlimitedCapacity;
}
