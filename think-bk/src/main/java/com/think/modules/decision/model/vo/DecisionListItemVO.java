package com.think.modules.decision.model.vo;

import com.think.modules.user.model.vo.UserBriefVO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 决策单列表项（我的 / 群内 / 公开广场通用）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DecisionListItemVO {
    /**
     * 决策单ID
     */
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 状态：0-草稿 1-评议中 2-已截止 3-已复盘 4-已关闭
     */
    private Integer status;
    /**
     * 可见范围：1-仅自己 2-群内 3-公开
     */
    private Integer visibility;
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
     * 冷静期截止时间
     */
    private LocalDateTime coolDownEndAt;
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
     * 评议人数上限
     */
    private Integer reviewLimit;
    /**
     * 发起人简要信息
     */
    private UserBriefVO initiator;
}
