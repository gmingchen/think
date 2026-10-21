package com.think.modules.abuse.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报信息
 *
 * @author 好市民
 */
@Data
public class AbuseReportDTO {
    /**
     * 举报ID
     */
    private Long id;
    /**
     * 举报对象类型：1-决策单 2-评议
     */
    private Integer targetType;
    /**
     * 对象ID
     */
    private Long targetId;
    /**
     * 举报人用户ID
     */
    private Long userId;
    /**
     * 举报理由
     */
    private String reason;
    /**
     * 处理状态：0-待处理 1-已处理(下架) 2-驳回
     */
    private Integer handleStatus;
    /**
     * 处理备注
     */
    private String handleNote;
    /**
     * 处理时间
     */
    private LocalDateTime handledAt;
}
