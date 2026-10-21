package com.think.modules.report.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 决策报告信息
 *
 * @author 好市民
 */
@Data
public class ReportDTO {
    /**
     * 报告ID
     */
    private Long id;
    /**
     * 决策单ID
     */
    private Long decisionId;
    /**
     * 各备选项有效人数（下标 -> 计数）
     */
    private Map<String, Integer> optionCounts;
    /**
     * 有效评议总数
     */
    private Integer totalReview;
    /**
     * 零评议AI兜底：0-否 1-是
     */
    private Integer isAiFallback;
    /**
     * AI三思参考正文
     */
    private String aiContent;
    /**
     * 报告生成时间
     */
    private LocalDateTime generatedAt;
}
