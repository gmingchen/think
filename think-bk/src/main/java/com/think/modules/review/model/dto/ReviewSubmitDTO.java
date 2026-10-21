package com.think.modules.review.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交/修改评议参数（一条有效，重复提交即修改本人评议）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class ReviewSubmitDTO {
    /**
     * 所属决策单ID
     */
    @NotNull(message = "决策单ID不能为空")
    private Long decisionId;
    /**
     * 倾向备选项下标（0-based）
     */
    @NotNull(message = "请选择倾向的备选项")
    private Integer option;
    /**
     * 评议观点文字（强制填写，不能为空）
     */
    @NotBlank(message = "请填写评议观点")
    @Size(max = 500, message = "评议观点不能超过500字")
    private String content;
}
