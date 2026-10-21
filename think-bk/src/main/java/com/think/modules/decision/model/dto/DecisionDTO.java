package com.think.modules.decision.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 决策单信息
 *
 * @author 好市民
 */
@Data
public class DecisionDTO {
    /**
     * 决策单ID
     */
    private Long id;
    /**
     * 发起人用户ID
     */
    private Long userId;
    /**
     * 标题
     */
    private String title;
    /**
     * 决策内容
     */
    private String content;
    /**
     * 备选方案数组
     */
    private List<DecisionOption> options;
    /**
     * 图片URL数组
     */
    private List<String> images;
    /**
     * 可见范围：1-仅自己 2-群内 3-公开
     */
    private Integer visibility;
    /**
     * 目标群ID
     */
    private Long grpId;
    /**
     * 冷静期时长(小时)
     */
    private Integer coolDownHours;
    /**
     * 冷静期截止时间
     */
    private LocalDateTime coolDownEndAt;
    /**
     * 评议人数上限
     */
    private Integer reviewLimit;
    /**
     * 状态：0-草稿 1-评议中 2-已截止 3-已复盘 4-已关闭
     */
    private Integer status;
    /**
     * 审核闸门状态
     */
    private Integer checkStatus;
    /**
     * 提前结束标记：0-否 1-是
     */
    private Integer endedEarly;
    /**
     * 报告是否已生成：0-否 1-是
     */
    private Integer reportGenerated;
    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
