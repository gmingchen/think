package com.think.modules.decision.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 发布决策单参数（草稿 -> 生效：群内直生效、公开走审核闸门）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DecisionPublishDTO {
    /**
     * 决策单ID
     */
    @NotNull(message = "决策单ID不能为空")
    private Long id;
}
