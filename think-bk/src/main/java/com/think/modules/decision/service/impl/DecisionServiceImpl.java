package com.think.modules.decision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.CheckStatusEnum;
import com.think.common.enums.ContentCheckResultEnum;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.GrpStatusEnum;
import com.think.common.enums.OperationActionEnum;
import com.think.common.enums.OperationBizTypeEnum;
import com.think.common.enums.ResultCodeEnum;
import com.think.common.enums.VisibilityEnum;
import com.think.common.pojo.PageResult;
import com.think.common.utils.CollectionUtils;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.security.utils.SecurityUtils;
import com.think.exception.RunException;
import com.think.modules.auth.model.LoginUser;
import com.think.modules.decision.entity.DecisionCheckEntity;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.entity.DecisionTagEntity;
import com.think.modules.decision.mapper.DecisionCheckMapper;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.decision.mapper.DecisionTagMapper;
import com.think.modules.decision.model.dto.DecisionCreateDTO;
import com.think.modules.decision.model.dto.DecisionGrpPageDTO;
import com.think.modules.decision.model.dto.DecisionIdDTO;
import com.think.modules.decision.model.dto.DecisionOption;
import com.think.modules.decision.model.dto.DecisionPageDTO;
import com.think.modules.decision.model.dto.DecisionPlazaPageDTO;
import com.think.modules.decision.model.dto.DecisionPublishDTO;
import com.think.modules.decision.model.dto.DecisionUpdateDTO;
import com.think.modules.decision.model.vo.DecisionDetailVO;
import com.think.modules.decision.model.vo.DecisionListItemVO;
import com.think.modules.decision.model.vo.ReviewItemVO;
import com.think.modules.decision.model.vo.UserStatsVO;
import com.think.modules.decision.service.DecisionService;
import com.think.modules.grp.entity.GrpEntity;
import com.think.modules.grp.mapper.GrpMapper;
import com.think.modules.grp.service.GrpMemberService;
import com.think.modules.log.service.OperationLogService;
import com.think.modules.recap.entity.RecapEntity;
import com.think.modules.recap.mapper.RecapMapper;
import com.think.modules.report.entity.ReportEntity;
import com.think.modules.report.mapper.ReportMapper;
import com.think.modules.report.service.ReportService;
import com.think.modules.review.entity.ReviewEntity;
import com.think.modules.review.mapper.ReviewMapper;
import com.think.modules.tag.entity.TagEntity;
import com.think.modules.tag.mapper.TagMapper;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.model.vo.UserBriefVO;
import com.think.modules.user.service.UserService;
import com.think.modules.wechat.service.AppletSecCheckService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 决策单
 *
 * @author 好市民
 */
@Service("decisionService")
public class DecisionServiceImpl extends ServiceImplX<DecisionMapper, DecisionEntity> implements DecisionService {

    /**
     * 公开档审核渠道标识
     */
    private static final String CHECK_PROVIDER = "wx";

    @Autowired
    private GrpMemberService grpMemberService;
    @Autowired
    private GrpMapper grpMapper;
    @Autowired
    private AppletSecCheckService appletSecCheckService;
    @Autowired
    private DecisionTagMapper decisionTagMapper;
    @Autowired
    private DecisionCheckMapper decisionCheckMapper;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private RecapMapper recapMapper;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private UserService userService;
    @Autowired
    private TagMapper tagMapper;

    // ================= 读 =================

    @Override
    public PageResult<DecisionListItemVO> pageMine(DecisionPageDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        LambdaQueryWrapper<DecisionEntity> wrapper = new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getUserId, userId)
                .eq(dto.getStatus() != null, DecisionEntity::getStatus, dto.getStatus())
                .eq(dto.getVisibility() != null, DecisionEntity::getVisibility, dto.getVisibility())
                .orderByDesc(DecisionEntity::getCreatedAt);
        return toPage(baseMapper.selectPage(dto, wrapper));
    }

    @Override
    public PageResult<DecisionListItemVO> pageReviewed(DecisionPageDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        List<Long> decisionIds = CollectionUtils.mapList(
                reviewMapper.selectList(new LambdaQueryWrapper<ReviewEntity>().eq(ReviewEntity::getUserId, userId)),
                ReviewEntity::getDecisionId).stream().distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(decisionIds)) {
            return new PageResult<DecisionListItemVO>(0L, 0L, Collections.emptyList()).setCurrent(dto.getCurrent());
        }
        LambdaQueryWrapper<DecisionEntity> wrapper = new LambdaQueryWrapper<DecisionEntity>()
                .in(DecisionEntity::getId, decisionIds)
                .eq(dto.getStatus() != null, DecisionEntity::getStatus, dto.getStatus())
                .orderByDesc(DecisionEntity::getCreatedAt);
        return toPage(baseMapper.selectPage(dto, wrapper));
    }

    @Override
    public PageResult<DecisionListItemVO> pageGrp(DecisionGrpPageDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        if (!grpMemberService.isMember(dto.getGrpId(), userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        LambdaQueryWrapper<DecisionEntity> wrapper = new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getGrpId, dto.getGrpId())
                .eq(DecisionEntity::getVisibility, VisibilityEnum.GRP.getCode())
                .eq(dto.getStatus() != null, DecisionEntity::getStatus, dto.getStatus())
                .orderByDesc(DecisionEntity::getCreatedAt);
        return toPage(baseMapper.selectPage(dto, wrapper));
    }

    @Override
    public PageResult<DecisionListItemVO> pagePlaza(DecisionPlazaPageDTO dto) {
        List<Long> tagDecisionIds = null;
        if (dto.getTagId() != null) {
            tagDecisionIds = CollectionUtils.mapList(decisionTagMapper.selectList(
                    new LambdaQueryWrapper<DecisionTagEntity>().eq(DecisionTagEntity::getTagId, dto.getTagId())),
                    DecisionTagEntity::getDecisionId);
            if (CollectionUtils.isEmpty(tagDecisionIds)) {
                return new PageResult<DecisionListItemVO>(0L, 0L, Collections.emptyList()).setCurrent(dto.getCurrent());
            }
        }
        LambdaQueryWrapper<DecisionEntity> wrapper = new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getVisibility, VisibilityEnum.PUBLIC.getCode())
                .eq(DecisionEntity::getCheckStatus, CheckStatusEnum.PASS.getCode())
                .in(DecisionEntity::getStatus, Arrays.asList(
                        DecisionStatusEnum.REVIEWING.getCode(),
                        DecisionStatusEnum.ENDED.getCode(),
                        DecisionStatusEnum.RECAPTURED.getCode()))
                .in(tagDecisionIds != null, DecisionEntity::getId, tagDecisionIds)
                .orderByDesc(DecisionEntity::getPublishedAt);
        return toPage(baseMapper.selectPage(dto, wrapper));
    }

    @Override
    public DecisionDetailVO randomPlaza() {
        List<DecisionEntity> pool = baseMapper.selectList(new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getVisibility, VisibilityEnum.PUBLIC.getCode())
                .eq(DecisionEntity::getCheckStatus, CheckStatusEnum.PASS.getCode())
                .eq(DecisionEntity::getStatus, DecisionStatusEnum.REVIEWING.getCode()));
        if (CollectionUtils.isEmpty(pool)) {
            return null;
        }
        DecisionEntity pick = pool.get(new Random().nextInt(pool.size()));
        return detail(pick.getId());
    }

    @Override
    public DecisionDetailVO detail(Long id) {
        LoginUser loginUser = requireLoginUser();
        Long me = loginUser.getId();
        DecisionEntity decision = baseMapper.selectById(id);
        if (decision == null) {
            throw new RunException("该决策单已被发起人删除或不存在");
        }
        boolean owner = decision.getUserId().equals(me);
        ReviewEntity myReviewEntity = reviewMapper.selectOne(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, id).eq(ReviewEntity::getUserId, me));
        boolean reviewer = myReviewEntity != null;

        // 可见性判定
        Integer visibility = decision.getVisibility();
        if (VisibilityEnum.SELF.getCode().equals(visibility) && !owner) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        boolean grpMember = false;
        if (VisibilityEnum.GRP.getCode().equals(visibility)) {
            grpMember = grpMemberService.isMember(decision.getGrpId(), me);
            if (!owner && !grpMember && !reviewer) {
                throw new RunException(ResultCodeEnum.NOT_ALLOWED);
            }
        }
        if (VisibilityEnum.PUBLIC.getCode().equals(visibility)
                && !owner && !CheckStatusEnum.PASS.getCode().equals(decision.getCheckStatus())) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }

        int reviewCount = countReviews(id);
        boolean canViewReviews = owner || reviewer;
        // 可评议：评议中 + 非自评 + 有访问权 + （已评议者可改 / 新评议者未满员）；群内档须群成员或已评议
        boolean accessOk = owner || grpMember || reviewer
                || VisibilityEnum.PUBLIC.getCode().equals(visibility);
        boolean canReview = DecisionStatusEnum.REVIEWING.getCode().equals(decision.getStatus())
                && !owner && accessOk
                && (reviewer || reviewCount < decision.getReviewLimit());

        // 组装评议列表（先评后看）
        List<ReviewEntity> allReviews = reviewMapper.selectList(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, id).orderByAsc(ReviewEntity::getCreatedAt));
        Map<Long, UserBriefVO> briefMap = loadBriefs(allReviews.stream()
                .map(ReviewEntity::getUserId).collect(Collectors.toList()));
        List<ReviewItemVO> reviews = Collections.emptyList();
        if (canViewReviews) {
            reviews = allReviews.stream()
                    .filter(r -> !r.getUserId().equals(me))
                    .filter(r -> !VisibilityEnum.PUBLIC.getCode().equals(visibility)
                            || CheckStatusEnum.PASS.getCode().equals(r.getCheckStatus()))
                    .map(r -> toReviewItem(r, briefMap))
                    .collect(Collectors.toList());
        }

        DecisionDetailVO vo = new DecisionDetailVO()
                .setId(decision.getId())
                .setUserId(decision.getUserId())
                .setInitiator(loadBrief(decision.getUserId()))
                .setTitle(decision.getTitle())
                .setContent(decision.getContent())
                .setOptions(decision.getOptions())
                .setImages(decision.getImages())
                .setTags(loadTagNames(id))
                .setVisibility(decision.getVisibility())
                .setGrpId(decision.getGrpId())
                .setCoolDownHours(decision.getCoolDownHours())
                .setCoolDownEndAt(decision.getCoolDownEndAt())
                .setReviewLimit(decision.getReviewLimit())
                .setStatus(decision.getStatus())
                .setCheckStatus(decision.getCheckStatus())
                .setEndedEarly(decision.getEndedEarly())
                .setReportGenerated(decision.getReportGenerated())
                .setPublishedAt(decision.getPublishedAt())
                .setCreatedAt(decision.getCreatedAt())
                .setReviewCount(reviewCount)
                .setOwner(owner)
                .setReviewer(reviewer)
                .setCanReview(canReview)
                .setCanViewReviews(canViewReviews)
                .setMyReview(myReviewEntity != null ? toReviewItem(myReviewEntity, briefMap) : null)
                .setReviews(reviews);
        // 报告仅发起人且已生成可见
        if (owner && Integer.valueOf(1).equals(decision.getReportGenerated())) {
            vo.setReport(reportService.infoByDecision(id));
        }
        return vo;
    }

    @Override
    public UserStatsVO statsMine() {
        Long me = SecurityUtils.getLoginUserId();
        long created = baseMapper.selectCount(new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getUserId, me));
        long reviewed = reviewMapper.selectCount(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getUserId, me));
        List<Long> myDecisionIds = CollectionUtils.mapList(
                baseMapper.selectList(new LambdaQueryWrapper<DecisionEntity>().eq(DecisionEntity::getUserId, me)),
                DecisionEntity::getId);
        long received = 0L;
        if (!CollectionUtils.isEmpty(myDecisionIds)) {
            received = reviewMapper.selectCount(new LambdaQueryWrapper<ReviewEntity>()
                    .in(ReviewEntity::getDecisionId, myDecisionIds));
        }
        return new UserStatsVO()
                .setCreatedCount((int) created)
                .setReviewedCount((int) reviewed)
                .setReceivedCount((int) received);
    }

    // ================= 写 =================

    @Override
    @Transactional(rollbackFor = RunException.class)
    public Long create(DecisionCreateDTO dto) {
        LoginUser loginUser = requireLoginUser();
        validateForSave(dto, loginUser.getId());
        DecisionEntity entity = new DecisionEntity()
                .setUserId(loginUser.getId())
                .setTitle(dto.getTitle())
                .setContent(dto.getContent())
                .setOptions(dto.getOptions())
                .setImages(dto.getImages())
                .setVisibility(dto.getVisibility())
                .setGrpId(dto.getGrpId())
                .setCoolDownHours(dto.getCoolDownHours())
                .setReviewLimit(dto.getReviewLimit())
                .setStatus(DecisionStatusEnum.DRAFT.getCode())
                .setCheckStatus(CheckStatusEnum.NONE.getCode())
                .setEndedEarly(0)
                .setReportGenerated(0);
        baseMapper.insert(entity);
        saveTags(entity.getId(), dto.getVisibility(), dto.getTagIds());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public void update(DecisionUpdateDTO dto) {
        LoginUser loginUser = requireLoginUser();
        DecisionEntity decision = validateExists(dto.getId());
        checkOwner(decision, loginUser.getId());
        checkEditable(decision);
        validateForSave(dto, loginUser.getId());
        decision.setTitle(dto.getTitle())
                .setContent(dto.getContent())
                .setOptions(dto.getOptions())
                .setImages(dto.getImages())
                .setVisibility(dto.getVisibility())
                .setGrpId(dto.getGrpId())
                .setCoolDownHours(dto.getCoolDownHours())
                .setReviewLimit(dto.getReviewLimit());
        baseMapper.updateById(decision);
        saveTags(decision.getId(), dto.getVisibility(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public void publish(DecisionPublishDTO dto) {
        LoginUser loginUser = requireLoginUser();
        DecisionEntity decision = validateExists(dto.getId());
        checkOwner(decision, loginUser.getId());
        if (!DecisionStatusEnum.DRAFT.getCode().equals(decision.getStatus())) {
            throw new RunException("该决策单不可重复发布");
        }
        Integer visibility = decision.getVisibility();
        if (VisibilityEnum.GRP.getCode().equals(visibility)) {
            validateGrp(decision.getGrpId(), loginUser.getId(), decision.getReviewLimit());
            activate(decision);
            baseMapper.updateById(decision);
            return;
        }
        if (VisibilityEnum.PUBLIC.getCode().equals(visibility)) {
            if (decision.getReviewLimit() > 100) {
                throw new RunException("公开档评议人数上限不能超过100");
            }
            decision.setCheckStatus(CheckStatusEnum.PENDING.getCode());
            try {
                appletSecCheckService.checkText(decision.getTitle() + "\n" + decision.getContent(), loginUser.getOpenid());
                decision.setCheckStatus(CheckStatusEnum.PASS.getCode());
                activate(decision);
                writeCheck(decision.getId(), ContentCheckResultEnum.PASS, "pass", "");
            } catch (RunException e) {
                decision.setCheckStatus(CheckStatusEnum.REJECT.getCode());
                writeCheck(decision.getId(), ContentCheckResultEnum.REJECT, "", e.getMessage());
            }
            baseMapper.updateById(decision);
            return;
        }
        throw new RunException("草稿仅自己可见，无需发布");
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public void endEarly(DecisionIdDTO dto) {
        LoginUser loginUser = requireLoginUser();
        DecisionEntity decision = validateExists(dto.getId());
        checkOwner(decision, loginUser.getId());
        if (!DecisionStatusEnum.REVIEWING.getCode().equals(decision.getStatus())) {
            throw new RunException("仅评议中的决策单可提前结束");
        }
        decision.setEndedEarly(1);
        baseMapper.updateById(decision);
        reportService.generate(decision.getId());
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public void close(DecisionIdDTO dto) {
        LoginUser loginUser = requireLoginUser();
        DecisionEntity decision = validateExists(dto.getId());
        checkOwner(decision, loginUser.getId());
        if (!DecisionStatusEnum.REVIEWING.getCode().equals(decision.getStatus())) {
            throw new RunException("仅评议中的决策单可关闭");
        }
        decision.setStatus(DecisionStatusEnum.CLOSED.getCode());
        baseMapper.updateById(decision);
        operationLogService.record(OperationActionEnum.DECISION_CLOSE, OperationBizTypeEnum.DECISION, decision.getId(), null);
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public void delete(DecisionIdDTO dto) {
        LoginUser loginUser = requireLoginUser();
        DecisionEntity decision = validateExists(dto.getId());
        checkOwner(decision, loginUser.getId());
        reviewMapper.delete(new LambdaQueryWrapper<ReviewEntity>().eq(ReviewEntity::getDecisionId, decision.getId()));
        reportMapper.delete(new LambdaQueryWrapper<ReportEntity>().eq(ReportEntity::getDecisionId, decision.getId()));
        recapMapper.delete(new LambdaQueryWrapper<RecapEntity>().eq(RecapEntity::getDecisionId, decision.getId()));
        decisionTagMapper.delete(new LambdaQueryWrapper<DecisionTagEntity>().eq(DecisionTagEntity::getDecisionId, decision.getId()));
        baseMapper.deleteById(decision.getId());
        operationLogService.record(OperationActionEnum.DECISION_DELETE, OperationBizTypeEnum.DECISION, decision.getId(), null);
    }

    // ================= 私有辅助 =================

    private void activate(DecisionEntity decision) {
        LocalDateTime now = LocalDateTime.now();
        decision.setStatus(DecisionStatusEnum.REVIEWING.getCode())
                .setPublishedAt(now)
                .setCoolDownEndAt(now.plusHours(decision.getCoolDownHours()));
    }

    private void validateForSave(DecisionCreateDTO dto, Long userId) {
        for (DecisionOption option : dto.getOptions()) {
            if (option == null || StringUtils.isBlank(option.getLabel())) {
                throw new RunException("备选方案内容不能为空");
            }
        }
        Integer visibility = dto.getVisibility();
        if (VisibilityEnum.GRP.getCode().equals(visibility)) {
            validateGrp(dto.getGrpId(), userId, dto.getReviewLimit());
        } else if (VisibilityEnum.PUBLIC.getCode().equals(visibility)) {
            if (dto.getReviewLimit() > 100) {
                throw new RunException("公开档评议人数上限不能超过100");
            }
        }
    }

    private void validateGrp(Long grpId, Long userId, Integer reviewLimit) {
        if (grpId == null) {
            throw new RunException("请选择目标群组");
        }
        GrpEntity grp = grpMapper.selectById(grpId);
        if (grp == null || !GrpStatusEnum.NORMAL.getCode().equals(grp.getStatus())) {
            throw new RunException("目标群组不存在或已解散");
        }
        if (!grpMemberService.isMember(grpId, userId)) {
            throw new RunException("你不是该群成员，无法发布到该群");
        }
        if (reviewLimit > grpMemberService.countByGrp(grpId)) {
            throw new RunException("评议人数上限不能超过当前群成员数");
        }
    }

    private void saveTags(Long decisionId, Integer visibility, List<Long> tagIds) {
        decisionTagMapper.delete(new LambdaQueryWrapper<DecisionTagEntity>()
                .eq(DecisionTagEntity::getDecisionId, decisionId));
        if (!VisibilityEnum.PUBLIC.getCode().equals(visibility) || CollectionUtils.isEmpty(tagIds)) {
            return;
        }
        tagIds.stream().filter(Objects::nonNull).distinct().forEach(tagId ->
                decisionTagMapper.insert(new DecisionTagEntity().setDecisionId(decisionId).setTagId(tagId)));
    }

    private void writeCheck(Long decisionId, ContentCheckResultEnum result, String suggest, String detail) {
        decisionCheckMapper.insert(new DecisionCheckEntity()
                .setDecisionId(decisionId)
                .setProvider(CHECK_PROVIDER)
                .setSuggest(suggest)
                .setCheckStatus(result.getCode())
                .setDetail(StringUtils.abbreviate(detail, 500))
                .setCheckedAt(LocalDateTime.now()));
    }

    private void checkEditable(DecisionEntity decision) {
        if (!DecisionStatusEnum.DRAFT.getCode().equals(decision.getStatus())
                || CheckStatusEnum.PENDING.getCode().equals(decision.getCheckStatus())) {
            throw new RunException("该决策单已生效或正在审核，不可编辑");
        }
    }

    private void checkOwner(DecisionEntity decision, Long userId) {
        if (!decision.getUserId().equals(userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new RunException(ResultCodeEnum.NOT_LOGIN);
        }
        return loginUser;
    }

    private int countReviews(Long decisionId) {
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, decisionId));
        return count == null ? 0 : count.intValue();
    }

    private List<String> loadTagNames(Long decisionId) {
        List<Long> tagIds = CollectionUtils.mapList(decisionTagMapper.selectList(
                new LambdaQueryWrapper<DecisionTagEntity>().eq(DecisionTagEntity::getDecisionId, decisionId)),
                DecisionTagEntity::getTagId);
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(TagEntity::getName).collect(Collectors.toList());
    }

    private UserBriefVO loadBrief(Long userId) {
        UserEntity user = userService.getById(userId);
        return user == null ? null : toBrief(user);
    }

    private Map<Long, UserBriefVO> loadBriefs(List<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, this::toBrief, (a, b) -> a));
    }

    private UserBriefVO toBrief(UserEntity user) {
        return new UserBriefVO().setId(user.getId()).setNickname(user.getNickname()).setAvatar(user.getAvatar());
    }

    private ReviewItemVO toReviewItem(ReviewEntity review, Map<Long, UserBriefVO> briefMap) {
        return new ReviewItemVO()
                .setId(review.getId())
                .setUserId(review.getUserId())
                .setOption(review.getOption())
                .setContent(review.getContent())
                .setCheckStatus(review.getCheckStatus())
                .setCreatedAt(review.getCreatedAt())
                .setUser(briefMap.get(review.getUserId()));
    }

    private DecisionListItemVO toItem(DecisionEntity decision, Map<Long, UserBriefVO> briefMap) {
        return new DecisionListItemVO()
                .setId(decision.getId())
                .setTitle(decision.getTitle())
                .setStatus(decision.getStatus())
                .setVisibility(decision.getVisibility())
                .setCheckStatus(decision.getCheckStatus())
                .setEndedEarly(decision.getEndedEarly())
                .setReportGenerated(decision.getReportGenerated())
                .setCoolDownEndAt(decision.getCoolDownEndAt())
                .setPublishedAt(decision.getPublishedAt())
                .setCreatedAt(decision.getCreatedAt())
                .setReviewCount(countReviews(decision.getId()))
                .setReviewLimit(decision.getReviewLimit())
                .setInitiator(briefMap.get(decision.getUserId()));
    }

    /**
     * 实体分页 -> 列表项分页（批量补发起人简要信息）
     */
    private PageResult<DecisionListItemVO> toPage(PageResult<DecisionEntity> entityPage) {
        List<DecisionEntity> entities = entityPage.getList();
        Map<Long, UserBriefVO> briefMap = loadBriefs(CollectionUtils.mapList(entities, DecisionEntity::getUserId));
        List<DecisionListItemVO> items = entities.stream()
                .map(decision -> toItem(decision, briefMap))
                .collect(Collectors.toList());
        return new PageResult<>(entityPage.getTotal(), entityPage.getPages(), items).setCurrent(entityPage.getCurrent());
    }

}
