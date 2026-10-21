package com.think.modules.review.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评议信息
 *
 * @author 好市民
 */
@Data
public class ReviewDTO {
    /**
     * 评议ID
     */
    private Long id;
    /**
     * 所属决策单ID
     */
    private Long decisionId;
    /**
     * 评议人用户ID
     */
    private Long userId;
    /**
     * 倾向备选项下标（0-based）
     */
    private Integer option;
    /**
     * 评议观点文字
     */
    private String content;
    /**
     * 审核闸门状态
     */
    private Integer checkStatus;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
