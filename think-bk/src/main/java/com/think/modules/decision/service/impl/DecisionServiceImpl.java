package com.think.modules.decision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.CheckStatusEnum;
import com.think.common.enums.ContentCheckResultEnum;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.GrpStatusEnum;
import com.think.common.enums.MediaBizTypeEnum;
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
import com.think.modules.wechat.service.AppletMediaCheckService;
import com.think.modules.wechat.service.AppletSecCheckService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
@Slf4j
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
    private AppletMediaCheckService appletMediaCheckService;
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
        Long me = SecurityUtils.getLoginUserId();
        // 当前用户已参与评议过的决策ID：广场需将其剔除（评过的不再展示）
        List<Long> reviewedIds = reviewedDecisionIds(me);
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
                // 广场不展示当前用户自己发布的决策（自己发的去「我的」看）
                .ne(DecisionEntity::getUserId, me)
                // 广场不展示当前用户已参与过评议的决策（评过的不再重复出现，去「我的-我评过的」看）
                .notIn(CollectionUtils.isNotEmpty(reviewedIds), DecisionEntity::getId, reviewedIds)
                // 众思广场只呈现「评议中」的公开决策（截止/复盘的不在广场展示），与「去看看」随机池同口径
                .eq(DecisionEntity::getStatus, DecisionStatusEnum.REVIEWING.getCode())
                .in(tagDecisionIds != null, DecisionEntity::getId, tagDecisionIds)
                .orderByDesc(DecisionEntity::getPublishedAt);
        return toPage(baseMapper.selectPage(dto, wrapper));
    }

    @Override
    public DecisionDetailVO randomPlaza() {
        Long me = SecurityUtils.getLoginUserId();
        List<Long> reviewedIds = reviewedDecisionIds(me);
        List<DecisionEntity> pool = baseMapper.selectList(new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getVisibility, VisibilityEnum.PUBLIC.getCode())
                .eq(DecisionEntity::getCheckStatus, CheckStatusEnum.PASS.getCode())
                .eq(DecisionEntity::getStatus, DecisionStatusEnum.REVIEWING.getCode())
                // 随机「去看看」同样排除当前用户自己的决策
                .ne(DecisionEntity::getUserId, me)
                // 已参与过评议的决策不再推来
                .notIn(CollectionUtils.isNotEmpty(reviewedIds), DecisionEntity::getId, reviewedIds));
        if (CollectionUtils.isEmpty(pool)) {
            return null;
        }
        DecisionEntity pick = pool.get(new Random().nextInt(pool.size()));
        return detail(pick.getId());
    }

    @Override
    public DecisionDetailVO changePlaza(Long excludeId) {
        Long me = SecurityUtils.getLoginUserId();
        List<Long> reviewedIds = reviewedDecisionIds(me);
        List<DecisionEntity> pool = baseMapper.selectList(new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getVisibility, VisibilityEnum.PUBLIC.getCode())
                .eq(DecisionEntity::getCheckStatus, CheckStatusEnum.PASS.getCode())
                .eq(DecisionEntity::getStatus, DecisionStatusEnum.REVIEWING.getCode())
                // 同样排除当前用户自己的决策
                .ne(DecisionEntity::getUserId, me)
                // 已参与过评议的决策不再推来
                .notIn(CollectionUtils.isNotEmpty(reviewedIds), DecisionEntity::getId, reviewedIds)
                // 「换一个」硬规则：在查询层直接排除上一条（excludeId），保证不与上一个重复；
                // 排除后无剩余则返回空（而非退回重复），由前端提示“没有更多可换”
                .ne(excludeId != null, DecisionEntity::getId, excludeId));
        if (CollectionUtils.isEmpty(pool)) {
            return null;
        }
        DecisionEntity pick = pool.get(new Random().nextInt(pool.size()));
        return detail(pick.getId());
    }

    /**
     * 当前用户已参与评议过的决策ID（去重），用于广场列表与随机池剔除“评过的不再显示”
     * @param me 当前登录用户ID
     * @return 决策ID列表，无则空列表
     */
    private List<Long> reviewedDecisionIds(Long me) {
        return CollectionUtils.mapList(
                reviewMapper.selectList(new LambdaQueryWrapper<ReviewEntity>().eq(ReviewEntity::getUserId, me)),
                ReviewEntity::getDecisionId).stream().distinct().collect(Collectors.toList());
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
                && (reviewer || isUnlimited(decision.getReviewLimit()) || reviewCount < decision.getReviewLimit());

        // 组装评议列表（先评后看）
        List<ReviewEntity> allReviews = reviewMapper.selectList(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getDecisionId, id).orderByAsc(ReviewEntity::getCreatedAt));
        Map<Long, UserBriefVO> briefMap = loadBriefs(allReviews.stream()
                .map(ReviewEntity::getUserId).collect(Collectors.toList()));
        List<ReviewItemVO> reviews = Collections.emptyList();
        if (canViewReviews) {
            reviews = allReviews.stream()
                    .filter(r -> !r.getUserId().equals(me))
                    // 审核不通过的评议对所有可见范围都隐藏（公开/群内均送审后拦截）
                    .filter(r -> !CheckStatusEnum.REJECT.getCode().equals(r.getCheckStatus()))
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
                .setCoolDownMinutes(decision.getCoolDownMinutes())
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
        // 一次取回我的全部决策单，发起数/复盘数/完成率均在内存上数，避免同表反复 count 往返
        List<DecisionEntity> myList = baseMapper.selectList(new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getUserId, me));
        long reviewed = reviewMapper.selectCount(new LambdaQueryWrapper<ReviewEntity>()
                .eq(ReviewEntity::getUserId, me));
        List<Long> myDecisionIds = CollectionUtils.mapList(myList, DecisionEntity::getId);
        long received = 0L;
        if (!CollectionUtils.isEmpty(myDecisionIds)) {
            received = reviewMapper.selectCount(new LambdaQueryWrapper<ReviewEntity>()
                    .in(ReviewEntity::getDecisionId, myDecisionIds));
        }
        // 复盘落库时已将 decision.status 置为已复盘（RecapServiceImpl#save），此处按状态直接数，无需 join recap 表
        int recap = CollectionUtils.filterList(myList,
                decision -> DecisionStatusEnum.RECAPTURED.getCode().equals(decision.getStatus())).size();
        return new UserStatsVO()
                .setCreatedCount(myList.size())
                .setReviewedCount((int) reviewed)
                .setReceivedCount((int) received)
                .setRecapCount(recap)
                .setReviewAvgRate(calcReviewAvgRate(myList));
    }

    /**
     * 结束评议单据的平均评议完成率（百分比整数，四舍五入），无符合条件单据返回 null。
     * <p>
     * 逐单算「实际评议数 / 该单 reviewLimit」再取算术平均：
     * 1）统计范围 = 已截止（未完成复盘）+ 已复盘（已完成复盘），即正常跑完评议流程的单据；
     * 已关闭不纳入（只能从评议中手动作废，或群解散级联关闭，不属于正常结束），评议中的单据分母未满属正常，纳入会虚低；
     * 2）分子封顶到分母，避免公开档超员提交把完成率顶到 100% 以上；
     * 3）reviewLimit 缺失或 <=0 的脏数据跳过该单，不计入均值也不除以 0。
     */
    private Integer calcReviewAvgRate(List<DecisionEntity> myList) {
        List<Integer> finishedStatuses = Arrays.asList(
                DecisionStatusEnum.ENDED.getCode(),
                DecisionStatusEnum.RECAPTURED.getCode());
        List<DecisionEntity> finishedList = CollectionUtils.filterList(myList,
                decision -> finishedStatuses.contains(decision.getStatus()));
        if (CollectionUtils.isEmpty(finishedList)) {
            return null;
        }
        List<Long> finishedIds = CollectionUtils.mapList(finishedList, DecisionEntity::getId);
        // 一次取回这些单据的评议再按 decisionId 在内存计数，避免逐单 count 的 N+1
        Map<Long, Long> reviewCountMap = reviewMapper.selectList(new LambdaQueryWrapper<ReviewEntity>()
                        .in(ReviewEntity::getDecisionId, finishedIds)
                        .select(ReviewEntity::getDecisionId))
                .stream()
                .collect(Collectors.groupingBy(ReviewEntity::getDecisionId, Collectors.counting()));
        double sum = 0d;
        int counted = 0;
        for (DecisionEntity decision : finishedList) {
            Integer limit = decision.getReviewLimit();
            if (limit == null || limit <= 0) {
                continue;
            }
            int actual = reviewCountMap.getOrDefault(decision.getId(), 0L).intValue();
            sum += Math.min(actual, limit) / (double) limit;
            counted++;
        }
        return counted == 0 ? null : (int) Math.round(sum / counted * 100);
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
                .setCoolDownMinutes(dto.getCoolDownMinutes())
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
                .setCoolDownMinutes(dto.getCoolDownMinutes())
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
        // 发布兼做兜底校验（正常不会触发：validateForSave 已在创建/更新拦住双不限；此处防历史脏数据）：时间不限与人数不限不可同时选
        boolean timeUnlimited = decision.getCoolDownMinutes() == null || decision.getCoolDownMinutes() <= 0;
        boolean countUnlimited = decision.getReviewLimit() == null || decision.getReviewLimit() <= 0;
        if (timeUnlimited && countUnlimited) {
            throw new RunException("评议时限与评议人数不能同时不限，请至少给一个设上限");
        }
        if (VisibilityEnum.GRP.getCode().equals(visibility)) {
            validateGrp(decision.getGrpId(), loginUser.getId(), decision.getReviewLimit());
            publishWithCheck(decision, loginUser.getOpenid());
            baseMapper.updateById(decision);
            return;
        }
        if (VisibilityEnum.PUBLIC.getCode().equals(visibility)) {
            if (countUnlimited) {
                throw new RunException("公开档需设置评议人数上限（不超过100人）");
            }
            if (decision.getReviewLimit() > 100) {
                throw new RunException("公开档评议人数上限不能超过100");
            }
            publishWithCheck(decision, loginUser.getOpenid());
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
        decision.setStatus(DecisionStatusEnum.REVIEWING.getCode()).setPublishedAt(now);
        // 不限时（cool_down_minutes=0）不产生到期时间：到期扫描依 cool_down_end_at 天然跳过，收口靠人数达标或手动结束
        Integer coolDownMinutes = decision.getCoolDownMinutes();
        decision.setCoolDownEndAt(coolDownMinutes == null || coolDownMinutes <= 0
                ? null : now.plusMinutes(coolDownMinutes));
    }

    private void validateForSave(DecisionCreateDTO dto, Long userId) {
        // 收口通道校验（任何场景禁止双不限，含存草稿）：评议时限与评议人数不能同时不限
        boolean timeUnlimited = dto.getCoolDownMinutes() == null || dto.getCoolDownMinutes() <= 0;
        boolean countUnlimited = dto.getReviewLimit() == null || dto.getReviewLimit() <= 0;
        if (timeUnlimited && countUnlimited) {
            throw new RunException("评议时限与评议人数不能同时不限，请至少给一个设上限");
        }
        for (String option : dto.getOptions()) {
            if (StringUtils.isBlank(option)) {
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
        // reviewLimit=0 为人数不限，不受群成员数封顶
        if (reviewLimit != null && reviewLimit > 0 && reviewLimit > grpMemberService.countByGrp(grpId)) {
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

    /**
     * 发布送审闸门（公开/群内均需，仅自己草稿不走发布）：标题+正文+选项合并送审。
     * 通过则激活并记 PASS；违规记 REJECT 且不激活（决策单维持草稿态，可编辑后重新发布）。
     */
    private void publishWithCheck(DecisionEntity decision, String openid) {
        decision.setCheckStatus(CheckStatusEnum.PENDING.getCode());
        try {
            appletSecCheckService.checkText(buildCheckText(decision), openid, AppletSecCheckService.SCENE_FORUM);
            decision.setCheckStatus(CheckStatusEnum.PASS.getCode());
            activate(decision);
            writeCheck(decision.getId(), ContentCheckResultEnum.PASS, "pass", "");
            // 文本通过并已激活后，图片逐张异步送审（先放行、回调命中再摘除）。
            // 登记到事务提交后再发，避免 mediaCheckAsync 的外部 HTTP 在发布事务内长时间占锁
            scheduleImageChecks(decision, openid);
        } catch (RunException e) {
            decision.setCheckStatus(CheckStatusEnum.REJECT.getCode());
            writeCheck(decision.getId(), ContentCheckResultEnum.REJECT, "", e.getMessage());
        }
    }

    /**
     * 把图片送审登记到事务提交之后触发：mediaCheckAsync 是外部 HTTP，放进带 @Transactional 的发布流程里，
     * 微信接口一旦变慢就会长时间占住事务与决策单行锁。仅当本次发布确实提交成功才真正送审；
     * 无图或当前不在事务同步语境时，直接提交。
     */
    private void scheduleImageChecks(DecisionEntity decision, String openid) {
        List<String> images = decision.getImages();
        if (CollectionUtils.isEmpty(images)) {
            return;
        }
        Long decisionId = decision.getId();
        List<String> snapshot = new ArrayList<>(images);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submitImageChecks(decisionId, snapshot, openid);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                submitImageChecks(decisionId, snapshot, openid);
            }
        });
    }

    /**
     * 逐张提交 mediaCheckAsync。整段兜异常：运行在事务提交之后，任何失败都不能再冒泡影响已成功的发布响应。
     */
    private void submitImageChecks(Long decisionId, List<String> images, String openid) {
        try {
            for (int i = 0; i < images.size(); i++) {
                String url = images.get(i);
                if (StringUtils.isNotBlank(url)) {
                    appletMediaCheckService.submitImageCheck(MediaBizTypeEnum.DECISION_IMAGE,
                            decisionId, i, url, openid, AppletSecCheckService.SCENE_FORUM);
                }
            }
        } catch (Exception e) {
            log.error("决策单图片送审登记失败（不影响已发布的决策单）：decisionId={}, err={}", decisionId, e.getMessage());
        }
    }

    /**
     * 拼接送审文本：标题 + 正文 + 各选项文案（选项同为 UGC，不能漏检）
     */
    private String buildCheckText(DecisionEntity decision) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.defaultString(decision.getTitle())).append('\n')
                .append(StringUtils.defaultString(decision.getContent()));
        List<String> options = decision.getOptions();
        if (options != null) {
            for (String option : options) {
                if (StringUtils.isNotBlank(option)) {
                    sb.append('\n').append(option);
                }
            }
        }
        return sb.toString();
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

    /**
     * 人数上限是否为「不限」哨兵值（0/null）
     */
    private static boolean isUnlimited(Integer reviewLimit) {
        return reviewLimit == null || reviewLimit <= 0;
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
                .setContent(decision.getContent())
                .setImages(decision.getImages())
                .setOptions(decision.getOptions())
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
