package com.think.modules.abuse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 举报（仅公开档；MVP 无运营后台，落库留痕、延后处置）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "abuse_report", autoResultMap = true)
public class AbuseReportEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 举报对象类型：1-决策单 2-评议（AbuseTargetTypeEnum）
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
     * 处理状态：0-待处理 1-已处理(下架) 2-驳回（AbuseHandleStatusEnum）
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
