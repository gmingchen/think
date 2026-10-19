package com.dishes.modules.ai.model.dto;

import com.dishes.modules.ai.model.vo.AiParseVO;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * AI 文案识别确认请求DTO（确认阶段）
 * <p>
 * 前端将 parse 返回的结构原样回传，后端此时才把名字解析进食材/调料库、匹配分类，
 * 返回带 id 的可回填结构（不写 dish 表，菜谱本体仍由保存接口落库）
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiParseCommitDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 菜名
     */
    @Size(max = 30, message = "菜名最多 30 字")
    private String name;
    /**
     * 分类提示（仅尝试匹配现有分类名）
     */
    private String categoryHint;
    /**
     * 备注 / 简介
     */
    @Size(max = 200, message = "备注最多 200 字")
    private String remark;
    /**
     * 食材（按名匹配/新建，命中复用）
     */
    @Size(max = 50, message = "食材最多 50 项")
    private List<AiParseVO.Food> foods;
    /**
     * 调料
     */
    @Size(max = 50, message = "调料最多 50 项")
    private List<AiParseVO.Seasoning> seasonings;
    /**
     * 步骤描述列表
     */
    @Size(max = 30, message = "步骤最多 30 步")
    private List<String> steps;
}
