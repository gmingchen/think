package com.think.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 操作审计日志（append-only，不随业务级联删除）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "operation_log", autoResultMap = true)
public class OperationLogEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 操作人用户ID（运营/管理员后台操作为 null）
     */
    private Long userId;
    /**
     * 操作类型（OperationActionEnum）
     */
    private String action;
    /**
     * 业务对象类型（OperationBizTypeEnum）
     */
    private String bizType;
    /**
     * 业务对象ID（无对象关联时置 0）
     */
    private Long bizId;
    /**
     * 操作详情
     */
    private String detail;
}
