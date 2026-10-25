package com.think.modules.recap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.ResultCodeEnum;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.exception.RunException;
import com.think.modules.auth.model.LoginUser;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.recap.convert.RecapConvert;
import com.think.modules.recap.entity.RecapEntity;
import com.think.modules.recap.mapper.RecapMapper;
import com.think.modules.recap.model.dto.RecapDTO;
import com.think.modules.recap.model.dto.RecapSaveDTO;
import com.think.modules.recap.service.RecapService;
import com.think.modules.wechat.service.AppletSecCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 决策复盘
 *
 * @author 好市民
 */
@Service("recapService")
public class RecapServiceImpl extends ServiceImplX<RecapMapper, RecapEntity> implements RecapService {

    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private AppletSecCheckService appletSecCheckService;

    @Override
    public RecapDTO infoByDecision(Long decisionId) {
        LambdaQueryWrapper<RecapEntity> wrapper = new LambdaQueryWrapper<RecapEntity>()
                .eq(RecapEntity::getDecisionId, decisionId);
        return Optional.ofNullable(baseMapper.selectOne(wrapper))
                .map(RecapConvert.INSTANCE::convert)
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public RecapDTO save(RecapSaveDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new RunException(ResultCodeEnum.NOT_LOGIN);
        }
        DecisionEntity decision = decisionMapper.selectById(dto.getDecisionId());
        if (decision == null) {
            throw new RunException(ResultCodeEnum.NOT_EXIST);
        }
        if (!decision.getUserId().equals(loginUser.getId())) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        // 仅已截止/已复盘可回填
        if (!DecisionStatusEnum.ENDED.getCode().equals(decision.getStatus())
                && !DecisionStatusEnum.RECAPTURED.getCode().equals(decision.getStatus())) {
            throw new RunException("决策单尚未截止，暂不能复盘");
        }
        // 最终选择下标校验（-1 表示其他/未选）
        if (dto.getFinalChoice() >= 0) {
            int size = decision.getOptions() == null ? 0 : decision.getOptions().size();
            if (dto.getFinalChoice() >= size) {
                throw new RunException("最终选择不合法");
            }
        }
        // 实际结局文本送审（命中违规抛异常→本次回填回滚；空文本由 checkText 内部跳过）
        appletSecCheckService.checkText(dto.getActualResult(), loginUser.getOpenid(), AppletSecCheckService.SCENE_SOCIAL);
        RecapEntity existing = baseMapper.selectOne(new LambdaQueryWrapper<RecapEntity>()
                .eq(RecapEntity::getDecisionId, dto.getDecisionId()));
        RecapEntity recap = existing != null ? existing : new RecapEntity().setDecisionId(dto.getDecisionId());
        recap.setFinalChoice(dto.getFinalChoice())
                .setActualResult(dto.getActualResult())
                .setReflectedAt(LocalDateTime.now());
        if (existing != null) {
            baseMapper.updateById(recap);
        } else {
            baseMapper.insert(recap);
        }
        // 回填后决策单进入已复盘
        decision.setStatus(DecisionStatusEnum.RECAPTURED.getCode());
        decisionMapper.updateById(decision);
        return RecapConvert.INSTANCE.convert(recap);
    }
}
