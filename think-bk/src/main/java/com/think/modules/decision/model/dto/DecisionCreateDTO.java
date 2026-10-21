package com.think.modules.decision.model.dto;

import com.think.common.enums.CoolDownHoursEnum;
import com.think.common.enums.VisibilityEnum;
import com.think.core.validator.constraints.Enum;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 创建决策单参数（§4.1）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DecisionCreateDTO {
    /**
     * 标题（≤30 字）
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 30, message = "标题不能超过30字")
    private String title;
    /**
     * 决策内容（自由描述两难处境）
     */
    @NotBlank(message = "决策内容不能为空")
    private String content;
    /**
     * 备选方案，≥2 项
     */
    @NotEmpty(message = "备选方案不能为空")
    @Size(min = 2, message = "备选方案至少2个")
    private List<DecisionOption> options;
    /**
     * 图片URL数组，≤3 张（可选）
     */
    @Size(max = 3, message = "图片最多3张")
    private List<String> images;
    /**
     * 可见范围：1-仅自己 2-群内 3-公开
     */
    @NotNull(message = "可见范围不能为空")
    @Enum(value = VisibilityEnum.class, message = "可见范围不合法")
    private Integer visibility;
    /**
     * 目标群ID（visibility=2 必填）
     */
    private Long grpId;
    /**
     * 冷静期时长(小时)：12/24/72/168
     */
    @NotNull(message = "冷静期时长不能为空")
    @Enum(value = CoolDownHoursEnum.class, message = "冷静期时长不合法")
    private Integer coolDownHours;
    /**
     * 评议人数上限：≥1
     */
    @NotNull(message = "评议人数上限不能为空")
    @Min(value = 1, message = "评议人数上限至少1人")
    private Integer reviewLimit;
    /**
     * 标签ID数组，≤5 个（仅公开档）
     */
    @Size(max = 5, message = "标签最多5个")
    private List<Long> tagIds;
}
