package com.think.modules.ai.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * AI 封面生图请求DTO
 * <p>
 * 由前端 add-edit 页封面区按钮触发，把当前 form 已有的 name/分类/食材/调料/步骤打包传后端；
 * 后端拼 prompt 调云函数 image-proxy 得 base64 dataURL 返回，本接口**不上传存储、不写 dish 表**，
 * 用户确认后才走现有 uploadMedia 上传得 https URL 回填 form.photo，菜谱仍由「保存」才落库
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiImageGenDTO {
    /**
     * 厨房ID（成员校验用，与 parse/commit 一致）
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 菜名（必填，prompt 主锚点）
     */
    @NotBlank(message = "请先填写菜名")
    @Size(max = 64, message = "菜名最多 64 字")
    private String name;
    /**
     * 分类提示（可空，辅助风格）
     */
    @Size(max = 32, message = "分类名最多 32 字")
    private String categoryHint;
    /**
     * 食材列表（可空；prompt 只取前 6 项名称+用量）
     * <p>
     * 这里的条数/长度上限只用于挡超大 payload，不参与业务判断：表单里填了多少项就该能生多少项，
     * 真正的取舍在 AiImagePrompt（每组前 6 条 + 500 字预算）里做
     * </p>
     */
    @Size(max = 100, message = "食材条目过多，请先精简")
    private List<FoodItem> foods;
    /**
     * 调料列表（可空；prompt 只取前 6 项名称）
     */
    @Size(max = 100, message = "调料条目过多，请先精简")
    private List<FoodItem> seasonings;

    /**
     * 步骤文本列表（可空；prompt 取前 6 条、每条只取首个分句，只为了体现炒/炖/蒸/烤这类决定成菜形态的做法）
     */
    @Size(max = 100, message = "步骤条目过多，请先精简")
    private List<String> steps;

    /**
     * 生图 prompt 里的食材/调料条目
     */
    @Data
    @Accessors(chain = true)
    public static class FoodItem {
        /**
         * 名称（空将被过滤掉，不进 prompt）
         */
        @Size(max = 64, message = "名称过长，请先精简")
        private String name;
        /**
         * 用量（调料可空）
         */
        @Size(max = 64, message = "用量过长，请先精简")
        private String amount;
    }
}
