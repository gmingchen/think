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
import org.springframework.context.annotation.Lazy;
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
    /**
     * 自身代理：save（无事务，含同步审核）内跨方法调用 saveInTx 以开启事务
     */
    @Autowired
    @Lazy
    private RecapServiceImpl self;

    @Override
    public RecapDTO infoByDecision(Long decisionId) {
        LambdaQueryWrapper<RecapEntity> wrapper = new LambdaQueryWrapper<RecapEntity>()
                .eq(RecapEntity::getDecisionId, decisionId);
        return Optional.ofNullable(baseMapper.selectOne(wrapper))
                .map(RecapConvert.INSTANCE::convert)
                .orElse(null);
    }

    @Override
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
        // 实际结局文本送审移出事务：不占用数据库连接等微信审核；命中违规抛异常拒绝，
        // 此时尚未落库（空文本由 checkText 内部跳过）
        appletSecCheckService.checkText(dto.getActualResult(), loginUser.getOpenid(), AppletSecCheckService.SCENE_SOCIAL);
        // 写库走事务：decision 行锁串行化「查已有 → 插入/更新 → 置已复盘」
        return self.saveInTx(dto, loginUser.getId());
    }

    /**
     * 回填复盘的写库事务（decision 行锁内执行，禁止直接调用）：行锁内复核归属与状态，
     * 拦截送审期间被并发处理的窗口；已有复盘改 UPDATE（一条有效），无则插入，
     * 并发重复提交在锁内天然串行为 UPDATE，不再撞 uk_decision 唯一键。
     */
    @Transactional(rollbackFor = RunException.class)
    public RecapDTO saveInTx(RecapSaveDTO dto, Long userId) {
        DecisionEntity decision = decisionMapper.selectByIdForUpdate(dto.getDecisionId());
        if (decision == null) {
            throw new RunException(ResultCodeEnum.NOT_EXIST);
        }
        if (!decision.getUserId().equals(userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        if (!DecisionStatusEnum.ENDED.getCode().equals(decision.getStatus())
                && !DecisionStatusEnum.RECAPTURED.getCode().equals(decision.getStatus())) {
            throw new RunException("决策单尚未截止，暂不能复盘");
        }
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
