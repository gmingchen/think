package com.think.modules.recap.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 决策复盘信息
 *
 * @author 好市民
 */
@Data
public class RecapDTO {
    /**
     * 复盘ID
     */
    private Long id;
    /**
     * 决策单ID
     */
    private Long decisionId;
    /**
     * 最终选择：options 下标（0-based）；-1=其他/未选
     */
    private Integer finalChoice;
    /**
     * 实际发生了什么
     */
    private String actualResult;
    /**
     * 回填时间
     */
    private LocalDateTime reflectedAt;
}
