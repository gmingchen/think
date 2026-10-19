package com.dishes.modules.ai.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * AI 荐菜采纳请求DTO
 * <p>
 * 只传推荐凭证与下标，菜谱内容取自 Redis 暂存，防前端篡改
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiAdoptDTO {
    /**
     * 推荐凭证
     */
    @NotBlank(message = "推荐凭证不能为空")
    private String recommendId;
    /**
     * 采纳的新菜谱下标（对应 recommend 返回的 fresh 数组）
     */
    @NotNull(message = "请选择要采纳的菜谱")
    private Integer index;
}
