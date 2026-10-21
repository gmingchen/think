package com.think.modules.abuse.service;

import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.abuse.entity.AbuseReportEntity;
import com.think.modules.abuse.model.dto.AbuseSubmitDTO;

/**
 * 举报
 *
 * @author 好市民
 */
public interface AbuseService extends IServiceX<AbuseReportEntity> {

    /**
     * 提交举报（落库留痕，待处理）
     * @param dto 举报参数
     * @return 举报ID
     */
    Long submit(AbuseSubmitDTO dto);
}
