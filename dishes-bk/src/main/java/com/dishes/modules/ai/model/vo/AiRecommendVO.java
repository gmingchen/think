package com.dishes.modules.ai.model.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 荐菜结果VO
 * @author 好市民
 */
@Data
public class AiRecommendVO {
    /**
     * 推荐凭证（Redis key，TTL 内可采纳）
     */
    private String recommendId;
    /**
     * 第一组：你家现有能做（LLM 排序 + 程序化求差得出有/缺食材）
     */
    private List<RankedDish> ranked;
    /**
     * 第二组：AI 帮你换新花样（生成型新菜谱，未落库）
     */
    private List<FreshDish> fresh;
    /**
     * 今日剩余次数
     */
    private Integer remainCount;

    /**
     * 第一组：库内推荐菜
     */
    @Data
    public static class RankedDish {
        /**
         * 菜谱ID
         */
        private Long dishId;
        /**
         * 菜名
         */
        private String dishName;
        /**
         * 封面图URL
         */
        private String dishCover;
        /**
         * 推荐理由（LLM 生成，口语化一句话）
         */
        private String reason;
        /**
         * 已有的食材名（程序化求交集）
         */
        private List<String> haveFoods;
        /**
         * 还缺的食材名（程序化求差集，防幻觉不吃 LLM 输出）
         */
        private List<String> lackFoods;
    }

    /**
     * 第二组：生成的新菜谱（结构为 DishCreateDTO 的按名裁剪版）
     */
    @Data
    public static class FreshDish {
        /**
         * 菜名
         */
        private String name;
        /**
         * 备注
         */
        private String remark;
        /**
         * 分类提示（仅尝试匹配现有分类名，匹配不到不设分类）
         */
        private String categoryHint;
        /**
         * 食材（按名匹配，采纳时命中复用/未命中新建）
         */
        private List<FreshFood> foods;
        /**
         * 调料
         */
        private List<FreshSeasoning> seasonings;
        /**
         * 步骤描述列表（顺序即步骤号）
         */
        private List<String> steps;
    }

    /**
     * 新菜谱-食材项
     */
    @Data
    public static class FreshFood {
        /**
         * 食材名
         */
        private String name;
        /**
         * 用量
         */
        private String amount;
        /**
         * 处理方式
         */
        private String process;
        /**
         * 是否可选
         */
        private Integer isOptional;
    }

    /**
     * 新菜谱-调料项
     */
    @Data
    public static class FreshSeasoning {
        /**
         * 调料名
         */
        private String name;
        /**
         * 用量
         */
        private String amount;
        /**
         * 是否可选
         */
        private Integer isOptional;
    }
}
