package com.think.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.ResultCodeEnum;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.exception.RunException;
import com.think.modules.ai.client.LlmClient;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.report.convert.ReportConvert;
import com.think.modules.report.entity.ReportEntity;
import com.think.modules.report.mapper.ReportMapper;
import com.think.modules.report.model.dto.ReportDTO;
import com.think.modules.report.prompt.ReportPrompt;
import com.think.modules.report.service.ReportService;
import com.think.modules.review.entity.ReviewEntity;
import com.think.modules.review.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 决策报告
 *
 * @author 好市民
 */
@Service("reportService")
public class ReportServiceImpl extends ServiceImplX<ReportMapper, ReportEntity> implements ReportService {

    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private LlmClient llmClient;

    @Override
    public ReportDTO infoByDecision(Long decisionId) {
        return Optional.ofNullable(selectByDecision(decisionId))
                .map(ReportConvert.INSTANCE::convert)
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public ReportDTO generate(Long decisionId) {
        DecisionEntity decision = decisionMapper.selectById(decisionId);
        if (decision == null) {
            throw new RunException(ResultCodeEnum.NOT_EXIST);
        }
        // 票数快照：以全部备选项下标初始化为 0，按有效评议逐条累加
        Map<String, Integer> counts = new LinkedHashMap<>();
        int optionSize = decision.getOptions() == null ? 0 : decision.getOptions().size();
        for (int i = 0; i < optionSize; i++) {
            counts.put(String.valueOf(i), 0);
        }
        List<ReviewEntity> reviews = reviewMapper.selectList(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, decisionId));
        for (ReviewEntity review : reviews) {
            counts.merge(String.valueOf(review.getOption()), 1, Integer::sum);
        }
        int total = reviews.size();

        ReportEntity report = selectByDecision(decisionId);
        boolean isNew = report == null;
        if (isNew) {
            report = new ReportEntity().setDecisionId(decisionId);
        }
        report.setOptionCounts(counts)
                .setTotalReview(total)
                .setGeneratedAt(LocalDateTime.now());
        // 零评议兜底：AI 三思参考替代空报告（§3.6）
        if (total == 0) {
            String ai = llmClient.chat(ReportPrompt.buildMessages(decision), null);
            report.setIsAiFallback(1).setAiContent(ai);
        } else {
            report.setIsAiFallback(0);
        }
        if (isNew) {
            baseMapper.insert(report);
        } else {
            baseMapper.updateById(report);
        }

        // 决策单置已截止并标记报告已生成（提前结束标记由调用方另行设置）
        decision.setStatus(DecisionStatusEnum.ENDED.getCode()).setReportGenerated(1);
        decisionMapper.updateById(decision);
        return ReportConvert.INSTANCE.convert(report);
    }

    private ReportEntity selectByDecision(Long decisionId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<ReportEntity>()
                .eq(ReportEntity::getDecisionId, decisionId));
    }
}
