package com.dishes.modules.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.constant.RedisConstant;
import com.dishes.common.enums.KitchenRoleEnum;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.redis.utils.RedisUtils;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.mapper.DishMapper;
import com.dishes.modules.kitchen.convert.KitchenConvert;
import com.dishes.modules.kitchen.entity.KitchenEntity;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.mapper.KitchenMapper;
import com.dishes.modules.kitchen.mapper.KitchenUserMapper;
import com.dishes.modules.kitchen.model.dto.KitchenUserCreateDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserDeleteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserInviteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserJoinDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserUpdateRoleDTO;
import com.dishes.modules.kitchen.model.vo.KitchenInviteInfoVO;
import com.dishes.modules.kitchen.model.vo.KitchenUserVO;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.user.entity.UserEntity;
import com.dishes.modules.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 厨房用户关联
 * @author 好市民
 */
@Slf4j
@Service("kitchenUserService")
public class KitchenUserServiceImpl extends ServiceImplX<KitchenUserMapper, KitchenUserEntity> implements KitchenUserService {

    /**
     * 邀请码有效期（秒）：1天
     */
    private static final long INVITE_CODE_EXPIRE = 24 * 60 * 60;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private KitchenMapper kitchenMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;
    
    @Override
    public List<KitchenUserVO> list(Long kitchenId) {
        return baseMapper.selectMemberList(kitchenId);
    }

    @Override
    public void create(KitchenUserCreateDTO dto) {
        // 厨房必须存活：软删除旧成员行可能来自「厨师长删库」连带清理，
        // 不校验就会把已删除厨房的成员关系重新激活成孤儿数据
        KitchenEntity kitchen = kitchenMapper.selectById(dto.getKitchenId());
        if (kitchen == null) {
            throw new RunException("厨房不存在");
        }
        // 查询含逻辑删除旧行：唯一索引 uk_kitchen_user 会占用软删除行，
        // 若只按 deleted=0 查重会漏判，导致后续 INSERT 撞键抛「已存在该记录」
        KitchenUserEntity exist = baseMapper.selectIncludeDeleted(dto.getKitchenId(), dto.getUserId());
        if (exist != null) {
            // 已在册（未删除）：幂等返回
            if (!Boolean.TRUE.equals(exist.getDeleted())) {
                return;
            }
            // 曾加入后退出（软删除旧行）：激活并更新本轮角色/邀请人
            // TODO 成员数上限暂未启用，待方案定稿后恢复下行校验
            // checkMemberLimit(kitchen);
            reactivate(exist, dto);
            return;
        }
        // 新增成员前校验成员数上限（与菜谱/分类上限同模式：上限存在厨房行值上）
        // TODO 成员数上限暂未启用，待方案定稿后恢复下行校验
        // checkMemberLimit(kitchen);
        KitchenUserEntity entity = KitchenConvert.INSTANCE.convert(dto);
        try {
            save(entity);
        } catch (DuplicateKeyException e) {
            // 并发首加：两请求同时读到 null 均走 INSERT，后到者撞唯一索引，
            // 回退为「已存在则激活/幂等返回」，避免撞键抛「已存在该记录」
            KitchenUserEntity again = baseMapper.selectIncludeDeleted(dto.getKitchenId(), dto.getUserId());
            if (again == null) {
                throw e;
            }
            if (!Boolean.TRUE.equals(again.getDeleted())) {
                return;
            }
            reactivate(again, dto);
        }
    }

    /**
     * 成员数上限校验：满员拒绝加入；存量超员的厨房因查重已在册幂等返回，不受影响
     * @param kitchen 目标厨房（行值存上限，与菜谱/分类同模式）
     */
    private void checkMemberLimit(KitchenEntity kitchen) {
        LambdaQueryWrapper<KitchenUserEntity> wrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, kitchen.getId());
        if (kitchen.getMaxMemberCount() != null && count(wrapper) >= kitchen.getMaxMemberCount()) {
            throw new RunException("成员数已达上限（" + kitchen.getMaxMemberCount() + "人）");
        }
    }

    /**
     * 激活软删除的旧成员行（重新加入），并发下以 WHERE deleted=1 做 CAS 兜底
     * @param exist 查到的旧行（deleted=1）
     * @param dto 本轮加入信息
     */
    private void reactivate(KitchenUserEntity exist, KitchenUserCreateDTO dto) {
        int rows = baseMapper.reactivateById(exist.getId(), dto.getRole(), dto.getInviterId(), LocalDateTime.now());
        if (rows == 0) {
            // 已被并发请求抢先激活：本次 role/inviterId 未生效，仅告警不阻断（用户已在册）
            log.warn("厨房成员激活 0 行（可能已被并发请求激活）kitchenId={}, userId={}, id={}",
                    dto.getKitchenId(), dto.getUserId(), exist.getId());
        }
    }

    @Override
    public void updateRole(KitchenUserUpdateRoleDTO dto) {
        Long currentUserId = SecurityUtils.getLoginUserId();
        KitchenUserEntity operator = checkMember(dto.getKitchenId(), currentUserId);
        if (!operator.getRole().equals(KitchenRoleEnum.OWNER.getCode())) {
            throw new RunException("只有厨师长才能修改角色");
        }
        LambdaUpdateWrapper<KitchenUserEntity> wrapper = new LambdaUpdateWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, dto.getKitchenId())
                .eq(KitchenUserEntity::getUserId, dto.getUserId())
                .set(KitchenUserEntity::getRole, dto.getRole());
        update(wrapper);
    }

    @Override
    public void delete(KitchenUserDeleteDTO dto) {
        checkPermission(dto.getKitchenId());
        LambdaQueryWrapper<KitchenUserEntity> wrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, dto.getKitchenId())
                .eq(KitchenUserEntity::getUserId, dto.getUserId());
        remove(wrapper);
    }

    @Override
    public String invite(KitchenUserInviteDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        checkPermission(dto.getKitchenId());
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        KitchenUserInviteDTO inviteDTO = new KitchenUserInviteDTO()
                .setKitchenId(dto.getKitchenId())
                .setInviterId(userId)
                .setRole(dto.getRole());
        redisUtils.set(RedisConstant.KITCHEN_INVITE + dto.getKitchenId() + ":" + code, inviteDTO, INVITE_CODE_EXPIRE);
        return code;
    }

    @Override
    public KitchenInviteInfoVO getInviteInfo(Long kitchenId, String code) {
        String key = RedisConstant.KITCHEN_INVITE + kitchenId + ":" + code;
        KitchenUserInviteDTO inviteDTO = redisUtils.get(key, KitchenUserInviteDTO.class)
                .orElseThrow(() -> new RunException("邀请码无效或已过期"));
        // 查询厨房信息
        KitchenEntity kitchen = kitchenMapper.selectById(inviteDTO.getKitchenId());
        if (kitchen == null) {
            throw new RunException("厨房不存在");
        }
        // 查询厨师长昵称
        UserEntity owner = userMapper.selectById(kitchen.getOwnerId());
        String ownerName = owner != null ? owner.getNickname() : "";
        // 查询邀请者昵称
        String inviterName = "";
        if (inviteDTO.getInviterId() != null) {
            UserEntity inviter = userMapper.selectById(inviteDTO.getInviterId());
            inviterName = inviter != null ? inviter.getNickname() : "";
        }
        // 查询成员数
        LambdaQueryWrapper<KitchenUserEntity> memberWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, inviteDTO.getKitchenId());
        int memberCount = (int) count(memberWrapper);
        // 查询菜谱数
        LambdaQueryWrapper<DishEntity> dishWrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, inviteDTO.getKitchenId());
        int dishCount = dishMapper.selectCount(dishWrapper).intValue();
        return new KitchenInviteInfoVO()
                .setKitchenId(inviteDTO.getKitchenId())
                .setKitchenName(kitchen.getName())
                .setOwnerName(ownerName)
                .setInviterName(inviterName)
                .setMemberCount(memberCount)
                .setDishCount(dishCount)
                .setRole(inviteDTO.getRole());
    }

    @Override
    public void join(KitchenUserJoinDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        String key = RedisConstant.KITCHEN_INVITE + dto.getKitchenId() + ":" + dto.getCode();
        KitchenUserInviteDTO inviteDTO = redisUtils.get(key, KitchenUserInviteDTO.class)
                .orElseThrow(() -> new RunException("邀请码无效或已过期"));
        // 检查是否已是成员
        LambdaQueryWrapper<KitchenUserEntity> wrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, inviteDTO.getKitchenId())
                .eq(KitchenUserEntity::getUserId, userId);
        if (count(wrapper) > 0) {
            throw new RunException("你已在该厨房");
        }
        KitchenUserCreateDTO createDTO = new KitchenUserCreateDTO()
                .setKitchenId(inviteDTO.getKitchenId())
                .setUserId(userId)
                .setRole(inviteDTO.getRole())
                .setInviterId(inviteDTO.getInviterId());
        create(createDTO);
    }

    @Override
    public void leave(Long kitchenId) {
        Long userId = SecurityUtils.getLoginUserId();
        KitchenUserEntity member = checkMember(kitchenId, userId);
        if (member.getRole().equals(KitchenRoleEnum.OWNER.getCode())) {
            throw new RunException("厨师长不能退出厨房");
        }
        removeById(member.getId());
    }

    @Override
    public KitchenUserEntity checkMember(Long kitchenId, Long userId) {
        LambdaQueryWrapper<KitchenUserEntity> wrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, kitchenId)
                .eq(KitchenUserEntity::getUserId, userId);
        KitchenUserEntity kitchenUserEntity = getOne(wrapper);
        if (kitchenUserEntity == null) {
            throw new RunException("您不是该厨房成员");
        }
        return kitchenUserEntity;
    }

    @Override
    public Set<Long> listKitchenIdsByUserId(Long userId) {
        LambdaQueryWrapper<KitchenUserEntity> wrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getUserId, userId);
        return list(wrapper).stream().map(KitchenUserEntity::getKitchenId).collect(Collectors.toSet());
    }

    @Override
    public KitchenUserEntity checkPermission(Long kitchenId) {
        KitchenUserEntity kitchenUserEntity = checkMember(kitchenId, SecurityUtils.getLoginUserId());
        int role = kitchenUserEntity.getRole();
        if (role != KitchenRoleEnum.OWNER.getCode() && role != KitchenRoleEnum.MANAGER.getCode()) {
            throw new RunException("您没有操作权限");
        }
        return kitchenUserEntity;
    }
}
