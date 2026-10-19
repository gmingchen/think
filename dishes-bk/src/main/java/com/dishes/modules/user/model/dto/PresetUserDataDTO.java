package com.dishes.modules.user.model.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 预设用户数据DTO
 * @author 好市民
 */
@Data
public class PresetUserDataDTO {
    /**
     * 厨房ID（由前端 service 自动注入）
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 分类名称列表
     */
    @NotEmpty(message = "分类不能为空")
    private List<String> categories;
    /**
     * 食材名称列表
     */
    private List<String> foods;
    /**
     * 调料名称列表
     */
    private List<String> seasonings;
}
