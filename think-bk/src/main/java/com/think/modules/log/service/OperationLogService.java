package com.think.modules.log.service;

import com.think.common.enums.OperationActionEnum;
import com.think.common.enums.OperationBizTypeEnum;
import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.log.entity.OperationLogEntity;

/**
 * 操作审计日志
 *
 * @author 好市民
 */
public interface OperationLogService extends IServiceX<OperationLogEntity> {

    /**
     * 记录一条操作审计（操作人取当前登录用户，可能为 null）
     * @param action  操作类型
     * @param bizType 业务对象类型
     * @param bizId   业务对象ID（无对象传 0）
     * @param detail  操作详情
     */
    void record(OperationActionEnum action, OperationBizTypeEnum bizType, Long bizId, String detail);
}
