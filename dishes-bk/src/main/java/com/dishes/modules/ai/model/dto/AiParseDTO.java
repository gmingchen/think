package com.dishes.modules.ai.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * AI 文案识别请求DTO（预览阶段）
 * <p>
 * 仅触发 LLM 解析，不建库、不落库；返回结构供弹窗预览
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiParseDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 做法文案（粘贴 / 输入的一段菜谱文字）
     */
    @NotBlank(message = "请输入要识别的文案")
    @Size(max = 1000, message = "文案最多 1000 字")
    private String text;
}
