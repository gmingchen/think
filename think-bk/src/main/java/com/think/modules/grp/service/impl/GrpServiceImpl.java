package com.think.modules.grp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.think.common.constant.RedisConstant;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.GrpMemberRoleEnum;
import com.think.common.enums.GrpStatusEnum;
import com.think.common.enums.ResultCodeEnum;
import com.think.common.pojo.PageResult;
import com.think.common.utils.CollectionUtils;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.core.redis.utils.RedisUtils;
import com.think.core.security.utils.SecurityUtils;
import com.think.exception.RunException;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.grp.entity.GrpEntity;
import com.think.modules.grp.mapper.GrpMapper;
import com.think.modules.grp.model.dto.GrpCreateDTO;
import com.think.modules.grp.model.dto.GrpIdDTO;
import com.think.modules.grp.model.dto.GrpJoinDTO;
import com.think.modules.grp.model.dto.GrpMemberDTO;
import com.think.modules.grp.model.dto.GrpPageDTO;
import com.think.modules.grp.model.dto.GrpRemoveMemberDTO;
import com.think.modules.grp.model.dto.GrpUpdateDTO;
import com.think.modules.grp.model.vo.GrpDetailVO;
import com.think.modules.grp.model.vo.GrpMemberVO;
import com.think.modules.grp.service.GrpMemberService;
import com.think.modules.grp.service.GrpService;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.model.vo.UserBriefVO;
import com.think.modules.user.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 群组
 *
 * @author 好市民
 */
@Service("grpService")
public class GrpServiceImpl extends ServiceImplX<GrpMapper, GrpEntity> implements GrpService {

    /**
     * 邀请码有效期（秒）：7 天
     */
    private static final long INVITE_EXPIRE = 7 * 24 * 60 * 60L;

    @Autowired
    private GrpMemberService grpMemberService;
    @Autowired
    private UserService userService;
    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public PageResult<GrpDetailVO> pageMy(GrpPageDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        List<Long> grpIds = grpMemberService.listGrpIdByUser(userId);
        if (CollectionUtils.isEmpty(grpIds)) {
            return new PageResult<GrpDetailVO>(0L, 0L, Collections.emptyList()).setCurrent(dto.getCurrent());
        }
        LambdaQueryWrapper<GrpEntity> wrapper = new LambdaQueryWrapper<GrpEntity>()
                .in(GrpEntity::getId, grpIds)
                .eq(GrpEntity::getStatus, GrpStatusEnum.NORMAL.getCode())
                .like(StringUtils.isNotBlank(dto.getKeyword()), GrpEntity::getName, dto.getKeyword())
                .orderByDesc(GrpEntity::getCreatedAt);
        PageResult<GrpEntity> entityPage = baseMapper.selectPage(dto, wrapper);
        List<GrpDetailVO> list = CollectionUtils.mapList(entityPage.getList(),
                grp -> toDetailVO(grp, userId));
        return new PageResult<>(entityPage.getTotal(), entityPage.getPages(), list)
                .setCurrent(entityPage.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public Long create(GrpCreateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        GrpEntity grp = new GrpEntity()
                .setName(dto.getName())
                .setAvatar(dto.getAvatar())
                .setUserId(userId)
                .setStatus(GrpStatusEnum.NORMAL.getCode());
        baseMapper.insert(grp);
        grpMemberService.addMember(grp.getId(), userId, GrpMemberRoleEnum.OWNER.getCode());
        return grp.getId();
    }

    @Override
    public GrpDetailVO detail(Long grpId) {
        GrpEntity grp = validateExists(grpId);
        Long userId = SecurityUtils.getLoginUserId();
        Integer myRole = grpMemberService.getRole(grpId, userId);
        if (myRole == null) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        return toDetailVO(grp, userId);
    }

    @Override
    public void update(GrpUpdateDTO dto) {
        GrpEntity grp = validateExists(dto.getGrpId());
        checkOwner(grp);
        baseMapper.update(new GrpEntity().setName(dto.getName()).setAvatar(dto.getAvatar()),
                new LambdaUpdateWrapper<GrpEntity>().eq(GrpEntity::getId, grp.getId()));
    }

    @Override
    @Transactional(rollbackFor = RunException.class)
    public void dissolve(GrpIdDTO dto) {
        GrpEntity grp = validateExists(dto.getGrpId());
        checkOwner(grp);
        baseMapper.update(new GrpEntity().setStatus(GrpStatusEnum.DISSOLVED.getCode()),
                new LambdaUpdateWrapper<GrpEntity>().eq(GrpEntity::getId, grp.getId()));
        // 级联关闭群内未完结决策单（草稿/评议中/已截止 -> 已关闭）
        decisionMapper.update(
                new DecisionEntity().setStatus(DecisionStatusEnum.CLOSED.getCode()),
                new LambdaUpdateWrapper<DecisionEntity>()
                        .eq(DecisionEntity::getGrpId, grp.getId())
                        .in(DecisionEntity::getStatus, Arrays.asList(
                                DecisionStatusEnum.DRAFT.getCode(),
                                DecisionStatusEnum.REVIEWING.getCode(),
                                DecisionStatusEnum.ENDED.getCode())));
    }

    @Override
    public String invite(GrpIdDTO dto) {
        GrpEntity grp = validateExists(dto.getGrpId());
        Long userId = SecurityUtils.getLoginUserId();
        if (!GrpStatusEnum.NORMAL.getCode().equals(grp.getStatus())
                || !grpMemberService.isMember(grp.getId(), userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        String code = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(RedisConstant.GRP_INVITE + code, grp.getId() + ":" + userId, INVITE_EXPIRE);
        return code;
    }

    @Override
    public void join(GrpJoinDTO dto) {
        Object value = redisUtils.get(RedisConstant.GRP_INVITE + dto.getCode()).orElse(null);
        if (value == null) {
            throw new RunException("邀请码已失效，请联系群主重新获取");
        }
        String[] parts = String.valueOf(value).split(":");
        Long grpId = Long.parseLong(parts[0]);
        GrpEntity grp = validateExists(grpId);
        if (!GrpStatusEnum.NORMAL.getCode().equals(grp.getStatus())) {
            throw new RunException("该群已解散");
        }
        Long userId = SecurityUtils.getLoginUserId();
        grpMemberService.addMember(grpId, userId, GrpMemberRoleEnum.MEMBER.getCode());
        redisUtils.delete(RedisConstant.GRP_INVITE + dto.getCode());
    }

    @Override
    public void leave(GrpIdDTO dto) {
        GrpEntity grp = validateExists(dto.getGrpId());
        Long userId = SecurityUtils.getLoginUserId();
        if (grp.getUserId().equals(userId)) {
            throw new RunException("群主不能退出群，请解散群");
        }
        if (!grpMemberService.isMember(grp.getId(), userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        grpMemberService.removeMember(grp.getId(), userId);
    }

    @Override
    public void removeMember(GrpRemoveMemberDTO dto) {
        GrpEntity grp = validateExists(dto.getGrpId());
        checkOwner(grp);
        if (grp.getUserId().equals(dto.getMemberId())) {
            throw new RunException("不能移除群主");
        }
        grpMemberService.removeMember(grp.getId(), dto.getMemberId());
    }

    @Override
    public List<GrpMemberVO> members(Long grpId) {
        validateExists(grpId);
        Long userId = SecurityUtils.getLoginUserId();
        if (!grpMemberService.isMember(grpId, userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
        List<GrpMemberDTO> memberList = grpMemberService.listByGrp(grpId);
        if (CollectionUtils.isEmpty(memberList)) {
            return Collections.emptyList();
        }
        List<Long> userIds = CollectionUtils.mapList(memberList, GrpMemberDTO::getUserId);
        Map<Long, UserBriefVO> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, this::toBrief, (a, b) -> a));
        return memberList.stream().map(member -> new GrpMemberVO()
                        .setUserId(member.getUserId())
                        .setRole(member.getRole())
                        .setJoinedAt(member.getCreatedAt())
                        .setUser(userMap.get(member.getUserId())))
                .collect(Collectors.toList());
    }

    /**
     * 群主校验
     */
    private void checkOwner(GrpEntity grp) {
        Long userId = SecurityUtils.getLoginUserId();
        if (!grp.getUserId().equals(userId)) {
            throw new RunException(ResultCodeEnum.NOT_ALLOWED);
        }
    }

    /**
     * 群实体 -> 详情VO（含实时成员数与我的角色）
     */
    private GrpDetailVO toDetailVO(GrpEntity grp, Long userId) {
        return new GrpDetailVO()
                .setId(grp.getId())
                .setName(grp.getName())
                .setAvatar(grp.getAvatar())
                .setUserId(grp.getUserId())
                .setStatus(grp.getStatus())
                .setMemberCount(grpMemberService.countByGrp(grp.getId()))
                .setMyRole(grpMemberService.getRole(grp.getId(), userId));
    }

    /**
     * 用户实体 -> 简要VO
     */
    private UserBriefVO toBrief(UserEntity user) {
        return new UserBriefVO()
                .setId(user.getId())
                .setNickname(user.getNickname())
                .setAvatar(user.getAvatar());
    }
}
