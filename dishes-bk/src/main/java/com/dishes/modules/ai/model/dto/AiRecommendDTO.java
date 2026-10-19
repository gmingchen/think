package com.dishes.modules.ai.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * AI 荐菜请求DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AiRecommendDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 勾选的食材ID（须属于该厨房，非本厨房 id 静默剔除）
     */
    @NotEmpty(message = "请至少选择一种食材")
    @Size(max = 30, message = "最多选择 30 种食材")
    private List<Long> foodIds;
    /**
     * 用餐人数（可选）
     */
    private Integer peopleCount;
    /**
     * 口味偏好（可选）
     */
    @Size(max = 20, message = "口味描述最多 20 字")
    private String taste;
    /**
     * 忌口（可选）
     */
    @Size(max = 20, message = "忌口描述最多 20 字")
    private String avoid;
    /**
     * 本会话已展示过的第一组菜谱 ID（换一批时从候选中排除以出不同的菜；候选耗尽则回退全量）
     */
    private List<Long> excludeDishIds;
    /**
     * 本会话已生成过的新菜谱名（换一批时并入 fresh 避开名单，以出不同的新菜）
     */
    private List<String> excludeFreshNames;
}
