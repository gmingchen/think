package com.think.modules.decision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 决策单内容审核流水（仅公开档；每次审核一行）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "decision_check", autoResultMap = true)
public class DecisionCheckEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 被审决策单ID
     */
    private Long decisionId;
    /**
     * 审核渠道（默认 wx）
     */
    private String provider;
    /**
     * 微信返回：pass/review/risky
     */
    private String suggest;
    /**
     * 单次审核结果：0-待审 1-通过 2-不通过 3-需人工复审（ContentCheckResultEnum）
     */
    private Integer checkStatus;
    /**
     * 命中标签/明细
     */
    private String detail;
    /**
     * 审核时间
     */
    private LocalDateTime checkedAt;
}
