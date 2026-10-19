package com.dishes.modules.seasoning.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调料VO
 * @author 好市民
 */
@Data
public class SeasoningVO {
    /**
     * ID
     */
    private Long id;
    /**
     * 调料名称
     */
    private String name;
    /**
     * 使用次数
     */
    private Integer usageCount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
