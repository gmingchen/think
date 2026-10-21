package com.think.modules.abuse.service.impl;

import com.think.common.enums.AbuseHandleStatusEnum;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.modules.abuse.entity.AbuseReportEntity;
import com.think.modules.abuse.mapper.AbuseReportMapper;
import com.think.modules.abuse.model.dto.AbuseSubmitDTO;
import com.think.modules.abuse.service.AbuseService;
import org.springframework.stereotype.Service;

/**
 * 举报
 *
 * @author 好市民
 */
@Service("abuseService")
public class AbuseServiceImpl extends ServiceImplX<AbuseReportMapper, AbuseReportEntity> implements AbuseService {

    @Override
    public Long submit(AbuseSubmitDTO dto) {
        AbuseReportEntity entity = new AbuseReportEntity()
                .setTargetType(dto.getTargetType())
                .setTargetId(dto.getTargetId())
                .setUserId(SecurityUtils.getLoginUserId())
                .setReason(dto.getReason())
                .setHandleStatus(AbuseHandleStatusEnum.PENDING.getCode());
        baseMapper.insert(entity);
        return entity.getId();
    }
}
