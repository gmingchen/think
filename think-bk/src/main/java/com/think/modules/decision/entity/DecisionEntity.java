package com.think.modules.decision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import com.think.core.mybatisplus.type.StringListTypeHandler;
import com.think.modules.decision.model.dto.DecisionOption;
import com.think.modules.decision.type.DecisionOptionListTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 决策单（§3.4 生命周期核心主表）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "decision", autoResultMap = true)
public class DecisionEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 发起人用户ID
     */
    private Long userId;
    /**
     * 决策单标题（<=30字）
     */
    private String title;
    /**
     * 决策内容（自由描述两难处境、背景与纠结点）
     */
    private String content;
    /**
     * 备选方案数组，>=2项；发布后锁定
     */
    @TableField(typeHandler = DecisionOptionListTypeHandler.class)
    private List<DecisionOption> options;
    /**
     * 决策单图片URL数组，最多3张（可选）
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> images;
    /**
     * 可见范围：1-仅自己(草稿) 2-群内 3-公开（VisibilityEnum）
     */
    private Integer visibility;
    /**
     * 目标群ID（visibility=2 必填）
     */
    private Long grpId;
    /**
     * 冷静期时长(小时)：12/24/72/168
     */
    private Integer coolDownHours;
    /**
     * 冷静期截止时间 = 发布时间 + 时长
     */
    private LocalDateTime coolDownEndAt;
    /**
     * 评议人数上限：>=1
     */
    private Integer reviewLimit;
    /**
     * 状态：0-草稿 1-评议中 2-已截止 3-已复盘 4-已关闭（DecisionStatusEnum）
     */
    private Integer status;
    /**
     * 审核闸门（仅公开档）：0-无需 1-待审 2-通过 3-不通过（CheckStatusEnum）
     */
    private Integer checkStatus;
    /**
     * 未满冷静期标记：0-否 1-提前结束
     */
    private Integer endedEarly;
    /**
     * 报告是否已生成：0-否 1-是
     */
    private Integer reportGenerated;
    /**
     * 发布时间（进入评议中时间）
     */
    private LocalDateTime publishedAt;
}
