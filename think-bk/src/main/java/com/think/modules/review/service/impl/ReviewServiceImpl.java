package com.think.modules.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.CheckStatusEnum;
import com.think.common.enums.ContentCheckResultEnum;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.ResultCodeEnum;
import com.think.common.enums.VisibilityEnum;
import com.think.common.pojo.PageResult;
import com.think.common.utils.CollectionUtils;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.exception.RunException;
import com.think.modules.auth.model.LoginUser;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.grp.service.GrpMemberService;
import com.think.modules.review.convert.ReviewConvert;
import com.think.modules.review.entity.ReviewCheckEntity;
import com.think.modules.review.entity.ReviewEntity;
import com.think.modules.review.mapper.ReviewCheckMapper;
import com.think.modules.review.mapper.ReviewMapper;
import com.think.modules.review.model.dto.ReviewDTO;
import com.think.modules.review.model.dto.ReviewPageDTO;
import com.think.modules.review.model.dto.ReviewSubmitDTO;
import com.think.modules.review.service.ReviewService;
import com.think.modules.report.service.ReportService;
import com.think.modules.wechat.service.AppletSecCheckService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评议
 *
 * @author 好市民
 */
@Service("reviewService")
public class ReviewServiceImpl extends ServiceImplX<ReviewMapper, ReviewEntity> implements ReviewService {

    private static final String CHECK_PROVIDER = "wx";

    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private GrpMemberService grpMemberService;
    @Autowired
    private AppletSecCheckService appletSecCheckService;
    @Autowired
    private ReviewCheckMapper reviewCheckMapper;
    @Autowired
    private ReportService reportService;
    /**
     * 自身代理：submit（无事务，含同步审核）内跨方法调用 submitInTx 以开启事务
     */
    @Autowired
    @Lazy
    private ReviewServiceImpl self;

    @Override
    public PageResult<ReviewDTO> pageByDecision(ReviewPageDTO dto) {
        LambdaQueryWrapper<ReviewEntity> wrapper = new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, dto.getDecisionId())
                .orderByDesc(ReviewEntity::getCreatedAt);
        PageResult<ReviewEntity> entityPage = baseMapper.selectPage(dto, wrapper);
        List<ReviewDTO> list = CollectionUtils.mapList(entityPage.getList(), ReviewConvert.INSTANCE::convert);
        return new PageResult<>(entityPage.getTotal(), entityPage.getPages(), list)
                .setCurrent(entityPage.getCurrent());
    }

    @Override
    public ReviewDTO submit(ReviewSubmitDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new RunException(ResultCodeEnum.NOT_LOGIN);
        }
        Long userId = loginUser.getId();
        DecisionEntity decision = decisionMapper.selectById(dto.getDecisionId());
        if (decision == null) {
            throw new RunException(ResultCodeEnum.NOT_EXIST);
        }
        if (!DecisionStatusEnum.REVIEWING.getCode().equals(decision.getStatus())) {
            throw new RunException("该决策单已截止或关闭，暂不可评议");
        }
        // 禁自评
        if (decision.getUserId().equals(userId)) {
            throw new RunException("不能评议自己的决策单");
        }
        // 群内档须群成员
        if (VisibilityEnum.GRP.getCode().equals(decision.getVisibility())
                && !grpMemberService.isMember(decision.getGrpId(), userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        // 倾向下标合法
        List<?> options = decision.getOptions();
        if (options == null || dto.getOption() < 0 || dto.getOption() >= options.size()) {
            throw new RunException("所选备选项不合法");
        }
        // 同步送审移出事务：不占用数据库连接等微信审核；命中违规时 checkText 抛 RunException，
        // 此时尚未落库、不占名额（与昵称/群名/复盘的拒绝口径一致）
        appletSecCheckService.checkText(dto.getContent(), loginUser.getOpenid(), AppletSecCheckService.SCENE_COMMENT);
        // 写库走事务：decision 行锁串行化「查已有/满员校验/插入/满员收口」
        return self.submitInTx(dto, userId);
    }

    /**
     * 提交评议的写库事务（decision 行锁内执行，禁止直接调用）：
     * 1）重查 status，拦截审核期间被到期扫描/他人满员收口截止的并发窗口；
     * 2）行锁内查已有 → 更新（一条有效），否则满员校验后插入（同用户并发重复提交在锁内天然串行为 UPDATE）；
     * 3）末位提交触发报告收口，与到期扫描/提前结束共享同一行锁串行化。
     */
    @Transactional(rollbackFor = RunException.class)
    public ReviewDTO submitInTx(ReviewSubmitDTO dto, Long userId) {
        DecisionEntity decision = decisionMapper.selectByIdForUpdate(dto.getDecisionId());
        if (decision == null) {
            throw new RunException(ResultCodeEnum.NOT_EXIST);
        }
        if (!DecisionStatusEnum.REVIEWING.getCode().equals(decision.getStatus())) {
            throw new RunException("该决策单已截止或关闭，暂不可评议");
        }
        ReviewEntity existing = baseMapper.selectOne(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, dto.getDecisionId())
                .eq(ReviewEntity::getUserId, userId));
        // 已有评议：修改（一条有效，UPDATE）。审核已在外层通过，直接落闸门态
        if (existing != null) {
            existing.setOption(dto.getOption()).setContent(dto.getContent())
                    .setCheckStatus(CheckStatusEnum.PASS.getCode());
            baseMapper.updateById(existing);
            writeCheck(existing.getId(), ContentCheckResultEnum.PASS, "pass", "");
            return ReviewConvert.INSTANCE.convert(existing);
        }
        // 新评议：满员校验（reviewLimit=0 为人数不限，不拦）
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, dto.getDecisionId()));
        long reviewed = count == null ? 0L : count;
        boolean limited = decision.getReviewLimit() != null && decision.getReviewLimit() > 0;
        if (limited && reviewed >= decision.getReviewLimit()) {
            throw new RunException(ResultCodeEnum.LIMIT_REACHED);
        }
        ReviewEntity entity = new ReviewEntity()
                .setDecisionId(dto.getDecisionId())
                .setUserId(userId)
                .setOption(dto.getOption())
                .setContent(dto.getContent())
                .setCheckStatus(CheckStatusEnum.PASS.getCode());
        baseMapper.insert(entity);
        writeCheck(entity.getId(), ContentCheckResultEnum.PASS, "pass", "");
        // 人数达标自动截止：设了人数上限的单据，末位评议提交后即出报告收口（与到期扫描/提前结束同一条生成链路）；
        // 报告生成失败则回滚本次提交，保证「评议落库 ⇔ 满员收口」原子一致
        if (limited && reviewed + 1 >= decision.getReviewLimit()) {
            reportService.generate(decision.getId());
        }
        return ReviewConvert.INSTANCE.convert(entity);
    }

    private void writeCheck(Long reviewId, ContentCheckResultEnum result, String suggest, String detail) {
        reviewCheckMapper.insert(new ReviewCheckEntity()
                .setReviewId(reviewId)
                .setProvider(CHECK_PROVIDER)
                .setSuggest(suggest)
                .setCheckStatus(result.getCode())
                .setDetail(StringUtils.abbreviate(detail, 500))
                .setCheckedAt(LocalDateTime.now()));
    }
}
