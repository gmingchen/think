package com.think.modules.decision.model.vo;

import com.think.modules.report.model.dto.ReportDTO;
import com.think.modules.user.model.vo.UserBriefVO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 决策单详情（含可见性判定结果、角色标记、先评后看的评议列表与报告）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DecisionDetailVO {
    /**
     * 决策单ID
     */
    private Long id;
    /**
     * 发起人用户ID
     */
    private Long userId;
    /**
     * 发起人简要信息
     */
    private UserBriefVO initiator;
    /**
     * 标题
     */
    private String title;
    /**
     * 决策内容
     */
    private String content;
    /**
     * 备选方案文案数组
     */
    private List<String> options;
    /**
     * 图片URL数组
     */
    private List<String> images;
    /**
     * 标签名称数组（仅公开档）
     */
    private List<String> tags;
    /**
     * 可见范围
     */
    private Integer visibility;
    /**
     * 目标群ID
     */
    private Long grpId;
    /**
     * 评议时限(分钟)：1~1440；0=不限
     */
    private Integer coolDownMinutes;
    /**
     * 评议时限截止时间
     */
    private LocalDateTime coolDownEndAt;
    /**
     * 评议人数上限
     */
    private Integer reviewLimit;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 审核闸门状态
     */
    private Integer checkStatus;
    /**
     * 提前结束标记
     */
    private Integer endedEarly;
    /**
     * 报告是否已生成
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
    /**
     * 有效评议数（实时统计）
     */
    private Integer reviewCount;
    /**
     * 当前用户是否为发起人
     */
    private Boolean owner;
    /**
     * 当前用户是否已评议
     */
    private Boolean reviewer;
    /**
     * 当前用户可评议（评议中 + 非自评 + 有访问权 + 未满员）
     */
    private Boolean canReview;
    /**
     * 当前用户可查看他人评议（先评后看：发起人或已提交评议者）
     */
    private Boolean canViewReviews;
    /**
     * 当前用户本人的评议（无则 null）
     */
    private ReviewItemVO myReview;
    /**
     * 他人评议列表（canViewReviews 时返回，否则为空）
     */
    private List<ReviewItemVO> reviews;
    /**
     * 决策报告（仅发起人且已生成时返回，否则 null）
     */
    private ReportDTO report;
}
