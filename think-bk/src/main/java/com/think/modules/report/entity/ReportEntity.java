package com.think.modules.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import com.think.modules.report.type.OptionCountMapTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 决策报告（截止/提前结束时生成；仅发起人可见）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "report", autoResultMap = true)
public class ReportEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 决策单ID（唯一）
     */
    private Long decisionId;
    /**
     * 各备选项有效人数（下标 -> 计数）
     */
    @TableField(typeHandler = OptionCountMapTypeHandler.class)
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
     * AI三思参考正文（仅兜底时存）
     */
    private String aiContent;
    /**
     * 报告生成时间
     */
    private LocalDateTime generatedAt;
}
