package com.think.modules.log.service.impl;

import com.think.common.enums.OperationActionEnum;
import com.think.common.enums.OperationBizTypeEnum;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.modules.log.entity.OperationLogEntity;
import com.think.modules.log.mapper.OperationLogMapper;
import com.think.modules.log.service.OperationLogService;
import org.springframework.stereotype.Service;

/**
 * 操作审计日志
 *
 * @author 好市民
 */
@Service("operationLogService")
public class OperationLogServiceImpl extends ServiceImplX<OperationLogMapper, OperationLogEntity> implements OperationLogService {

    @Override
    public void record(OperationActionEnum action, OperationBizTypeEnum bizType, Long bizId, String detail) {
        OperationLogEntity entity = new OperationLogEntity()
                .setUserId(SecurityUtils.getLoginUserId())
                .setAction(action.getCode())
                .setBizType(bizType != null ? bizType.getCode() : "")
                .setBizId(bizId != null ? bizId : 0L)
                .setDetail(detail != null ? detail : "");
        baseMapper.insert(entity);
    }
}
