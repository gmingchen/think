package com.think.modules.recap.service;

import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.recap.entity.RecapEntity;
import com.think.modules.recap.model.dto.RecapDTO;
import com.think.modules.recap.model.dto.RecapSaveDTO;

/**
 * 决策复盘
 *
 * @author 好市民
 */
public interface RecapService extends IServiceX<RecapEntity> {

    /**
     * 按决策单查询复盘
     * @param decisionId 决策单ID
     * @return 复盘信息，未回填时为 null
     */
    RecapDTO infoByDecision(Long decisionId);

    /**
     * 回填/修改复盘（仅发起人、决策单已截止或已复盘），回填后置 decision.status=3
     * @param dto 参数
     * @return 复盘信息
     */
    RecapDTO save(RecapSaveDTO dto);
}
