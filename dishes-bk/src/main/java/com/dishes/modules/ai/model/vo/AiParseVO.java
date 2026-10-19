package com.dishes.modules.ai.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI 文案识别结果VO
 * <p>
 * parse 返回仅含名字/分类提示（无 id，供弹窗预览）；commit 在此基础上把名字解析进食材/调料库、
 * 匹配分类后回填 foodId/seasoningId/categoryId/categoryName（供新增/编辑表单直接填充）
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiParseVO {
    /**
     * 菜名
     */
    private String name;
    /**
     * 分类提示（parse 输出，LLM 原样；commit 保留）
     */
    private String categoryHint;
    /**
     * 匹配到的分类ID（仅 commit 填，匹配不到为 null）
     */
    private Long categoryId;
    /**
     * 匹配到的分类名（仅 commit 填）
     */
    private String categoryName;
    /**
     * 备注 / 简介
     */
    private String remark;
    /**
     * 食材
     */
    private List<Food> foods;
    /**
     * 调料
     */
    private List<Seasoning> seasonings;
    /**
     * 步骤描述列表（顺序即步骤号）
     */
    private List<String> steps;

    /**
     * 食材项（foodId 仅 commit 填）
     */
    @Data
    @Accessors(chain = true)
    public static class Food {
        /**
         * 食材ID（commit 建库/命中后回填）
         */
        private Long foodId;
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
     * 调料项（seasoningId 仅 commit 填）
     */
    @Data
    @Accessors(chain = true)
    public static class Seasoning {
        /**
         * 调料ID（commit 建库/命中后回填）
         */
        private Long seasoningId;
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
