package com.dishes.modules.dish.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜谱分享VO
 * @author 好市民
 */
@Data
public class DishShareVO {
    /**
     * 菜谱ID
     */
    private Long id;
    /**
     * 菜名
     */
    private String name;
    /**
     * 封面图URL
     */
    private String cover;
    /**
     * 分类名称
     */
    private String categoryName;
    /**
     * 平均评分
     */
    private BigDecimal rating;
    /**
     * 做过次数
     */
    private Integer cookCount;
    /**
     * 被收藏次数
     */
    private Integer collectCount;
    /**
     * 备注/小贴士
     */
    private String remark;
    /**
     * 食材列表
     */
    private List<DishFoodVO> foods;
    /**
     * 调料列表
     */
    private List<DishSeasoningVO> seasonings;
    /**
     * 步骤列表
     */
    private List<DishStepVO> steps;
    /**
     * 分享者信息
     */
    private SharerInfo sharer;
    /**
     * 分享者厨房菜谱总数
     */
    private Integer dishCount;
    /**
     * 该菜谱已被收藏到的厨房ID列表（供前端标记「已收藏」）
     */
    private List<Long> collectedKitchens;

    /**
     * 分享者信息
     */
    @Data
    @Accessors(chain = true)
    public static class SharerInfo {
        /**
         * 昵称
         */
        private String nickname;
        /**
         * 头像
         */
        private String avatar;
        /**
         * 厨房名称
         */
        private String kitchenName;
        /**
         * 分享者在该厨房的角色：1-厨师长 2-管厨 3-食客（已退出厨房时为空）
         */
        private Integer role;
    }
}
