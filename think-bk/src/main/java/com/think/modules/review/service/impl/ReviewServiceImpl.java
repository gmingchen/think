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
    @Transactional(rollbackFor = RunException.class)
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
        boolean isPublic = VisibilityEnum.PUBLIC.getCode().equals(decision.getVisibility());

        ReviewEntity existing = baseMapper.selectOne(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, dto.getDecisionId())
                .eq(ReviewEntity::getUserId, userId));
        // 已有评议：修改（一条有效，UPDATE）
        if (existing != null) {
            existing.setOption(dto.getOption()).setContent(dto.getContent());
            if (isPublic) {
                existing.setCheckStatus(CheckStatusEnum.PENDING.getCode());
            }
            baseMapper.updateById(existing);
            if (isPublic) {
                existing.setCheckStatus(checkPublic(existing.getId(), dto.getContent(), loginUser.getOpenid()));
                baseMapper.updateById(existing);
            }
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
                .setCheckStatus(isPublic ? CheckStatusEnum.PENDING.getCode() : CheckStatusEnum.NONE.getCode());
        baseMapper.insert(entity);
        if (isPublic) {
            entity.setCheckStatus(checkPublic(entity.getId(), dto.getContent(), loginUser.getOpenid()));
            baseMapper.updateById(entity);
        }
        // 人数达标自动截止：设了人数上限的单据，末位评议提交后即出报告收口（与到期扫描/提前结束同一条生成链路）；
        // 报告生成失败则回滚本次提交，保证「评议落库 ⇔ 满员收口」原子一致
        if (limited && reviewed + 1 >= decision.getReviewLimit()) {
            reportService.generate(decision.getId());
        }
        return ReviewConvert.INSTANCE.convert(entity);
    }

    /**
     * 公开档评议同步送审：返回闸门态（通过=2 / 不通过=3），并写审核流水
     */
    private Integer checkPublic(Long reviewId, String content, String openid) {
        try {
            appletSecCheckService.checkText(content, openid);
            writeCheck(reviewId, ContentCheckResultEnum.PASS, "pass", "");
            return CheckStatusEnum.PASS.getCode();
        } catch (RunException e) {
            writeCheck(reviewId, ContentCheckResultEnum.REJECT, "", e.getMessage());
            return CheckStatusEnum.REJECT.getCode();
        }
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
