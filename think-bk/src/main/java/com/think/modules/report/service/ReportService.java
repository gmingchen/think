package com.think.modules.report.service;

import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.report.entity.ReportEntity;
import com.think.modules.report.model.dto.ReportDTO;

/**
 * 决策报告
 *
 * @author 好市民
 */
public interface ReportService extends IServiceX<ReportEntity> {

    /**
     * 按决策单查询报告
     * @param decisionId 决策单ID
     * @return 报告信息，未生成时为 null
     */
    ReportDTO infoByDecision(Long decisionId);

    /**
     * 生成决策报告（截止/提前结束时调用）：票数汇总快照 + 零评议 AI 三思兜底，
     * 并将决策单置为已截止（status=2）、report_generated=1
     * @param decisionId 决策单ID
     * @return 报告信息
     */
    ReportDTO generate(Long decisionId);
}
