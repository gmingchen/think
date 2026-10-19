package com.dishes.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.constant.RedisConstant;
import com.dishes.common.enums.KitchenRoleEnum;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.category.entity.CategoryEntity;
import com.dishes.modules.category.mapper.CategoryMapper;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.mapper.FoodMapper;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.service.KitchenService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.member.entity.MemberEntity;
import com.dishes.modules.member.mapper.MemberMapper;
import com.dishes.modules.os.service.StorageService;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.mapper.SeasoningMapper;
import com.dishes.modules.user.convert.UserConvert;
import com.dishes.modules.user.entity.UserEntity;
import com.dishes.modules.user.mapper.UserMapper;
import com.dishes.modules.user.model.dto.PresetUserDataDTO;
import com.dishes.modules.user.model.dto.UserCreateDTO;
import com.dishes.modules.user.model.dto.UserDTO;
import com.dishes.modules.user.model.dto.UserSetLastKitchenDTO;
import com.dishes.modules.user.model.dto.UserUpdateDTO;
import com.dishes.modules.user.service.UserService;
import com.dishes.modules.wechat.service.AppletCodeAndLinkService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dishes.core.redis.utils.RedisUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * 用户
 * @author 拖孩
 * @email 1240235512@qq.com
 * @date 2023-07-22 17:36:32
 */
@Service("userService")
public class UserServiceImpl extends ServiceImplX<UserMapper, UserEntity> implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private KitchenService kitchenService;
    @Autowired
    private KitchenUserService kitchenUserService;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private FoodMapper foodMapper;
    @Autowired
    private SeasoningMapper seasoningMapper;
    /**
     * 会员状态：只注 mapper 不注 MemberService，避免 member 与 user 两条服务链绕在一起（docs/v12 §9.2）
     */
    @Autowired
    private MemberMapper memberMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private StorageService storageService;
    @Autowired
    private AppletCodeAndLinkService appletCodeAndLinkService;

    @Override
    public UserDTO info(Long id) {
        UserDTO dto = Optional.ofNullable(baseMapper.selectById(id))
                .map(UserConvert.INSTANCE::convert)
                .orElse(null);
        if (dto != null) {
            fillMember(dto, id);
        }
        return dto;
    }

    /**
     * 把会员状态填进 UserDTO，随之进入 Redis 的 {@code user:{userId}} 快照与 {@code LoginUser}
     * <p>
     * 校验点（免广告、无限容量）就是靠这份快照做到零额外查询（docs/v12 §6.3）。
     * </p>
     *
     * @param dto    待填的用户DTO
     * @param userId 用户ID
     */
    private void fillMember(UserDTO dto, Long userId) {
        MemberEntity row = memberMapper.selectOne(new LambdaQueryWrapper<MemberEntity>()
                .eq(MemberEntity::getUserId, userId));
        // ⚠️ 必须先按 expire_at 归零再透出，不归零的话一个过期会员的 memberTier 永远是 2，
        // 而前端全站判据是 !user.memberTier → 他永久免广告、永久免激励视频（§8.1 / §17 第 25 条）
        dto.setMemberTier(row == null ? 0 : row.effectiveTier(LocalDateTime.now()));
        // 到期时间给原值而不是归零后的值：过期态的「已于 X 到期」这句话要用它
        dto.setMemberExpireAt(row == null ? null : row.getExpireAt());
    }

    @Override
    public UserDTO infoByOpenid(String openid) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getOpenid, openid);
        return Optional.ofNullable(baseMapper.selectOne(wrapper))
                .map(UserConvert.INSTANCE::convert)
                .orElse(null);
    }

    @Override
    public Long create(UserCreateDTO dto) {
        UserEntity userEntity = new UserEntity()
                .setOpenid(dto.getOpenid())
                .setUnionid(dto.getUnionid())
                .setNickname("厨师长-" + LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond())
                .setInviterId(dto.getInviterId());
        baseMapper.insert(userEntity);
        Long userId = userEntity.getId();
        // 邀请人计数 +1
        Long inviterId = dto.getInviterId();
        if (inviterId != null) {
            UserEntity inviterEntity = getById(inviterId);
            if (inviterEntity != null) {
                int currentCount = inviterEntity.getInviteCount() != null ? inviterEntity.getInviteCount() : 0;
                UserEntity updateEntity = new UserEntity()
                        .setId(inviterId)
                        .setInviteCount(currentCount + 1);
                updateById(updateEntity);
            }
        }
        // 创建默认厨房
        kitchenService.createDefault(userId);
        return userId;
    }

    @Override
    public void update(UserUpdateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        // 对比旧头像，头像变更时删除旧文件
        if (dto.getAvatar() != null) {
            UserEntity oldEntity = validateExists(userId);
            if (oldEntity.getAvatar() != null && !oldEntity.getAvatar().equals(dto.getAvatar())) {
                storageService.deleteByUrl(oldEntity.getAvatar());
            }
        }
        UserEntity entity = UserConvert.INSTANCE.convertEntity(dto)
                .setId(userId);
        baseMapper.updateById(entity);
        // 清除用户缓存
        redisUtils.delete(RedisConstant.USER + userId);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void presetUserData(PresetUserDataDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        Long kitchenId = dto.getKitchenId();
        // 批量创建分类（需要厨师长身份）
        if (CollectionUtils.isNotEmpty(dto.getCategories())) {
            KitchenUserEntity member = kitchenUserService.checkMember(kitchenId, userId);
            if (!member.getRole().equals(KitchenRoleEnum.OWNER.getCode())) {
                throw new RunException("只有厨师长才能预设分类");
            }
            List<CategoryEntity> categories = new ArrayList<>();
            for (int i = 0; i < dto.getCategories().size(); i++) {
                categories.add(new CategoryEntity()
                        .setKitchenId(kitchenId)
                        .setName(dto.getCategories().get(i))
                        .setSort(i + 1));
            }
            categoryMapper.insertBatch(categories);
        }
        // 批量创建食材
        if (CollectionUtils.isNotEmpty(dto.getFoods())) {
            List<FoodEntity> foods = new ArrayList<>();
            for (String name : dto.getFoods()) {
                foods.add(new FoodEntity()
                        .setKitchenId(kitchenId)
                        .setUserId(userId)
                        .setName(name)
                        .setUsageCount(0));
            }
            foodMapper.insertBatch(foods);
        }
        // 批量创建调料
        if (CollectionUtils.isNotEmpty(dto.getSeasonings())) {
            List<SeasoningEntity> seasonings = new ArrayList<>();
            for (String name : dto.getSeasonings()) {
                seasonings.add(new SeasoningEntity()
                        .setKitchenId(kitchenId)
                        .setUserId(userId)
                        .setName(name)
                        .setUsageCount(0));
            }
            seasoningMapper.insertBatch(seasonings);
        }
    }

    @Override
    public String generateQrcode(String page, Long userId) {
        // scene ≤ 32 字符：仅带邀请人 ID，与全站现有 inviterId 归因机制对齐
        byte[] bytes = appletCodeAndLinkService.getUnlimitedQrcodeByte("inviterId=" + userId, page);
        if (isWechatError(bytes)) {
            String body = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
            log.warn("小程序码生成失败, userId={}, page={}, resp={}", userId, page, body);
            throw new RunException("小程序码生成失败");
        }
        // 直接以 dataURL 返回，不落库、不传七牛
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 微信 getUnlimited 成功返回 PNG 二进制、失败返回 JSON 错误体，按首字节区分
     */
    private boolean isWechatError(byte[] bytes) {
        return bytes == null || bytes.length < 100 || bytes[0] == '{';
    }

    @Override
    public void updateLastKitchenId(UserSetLastKitchenDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        Long kitchenId = dto.getKitchenId();
        // 校验用户属于该厨房
        kitchenUserService.checkMember(kitchenId, userId);
        UserEntity entity = new UserEntity()
                .setId(userId)
                .setLastKitchenId(kitchenId);
        baseMapper.updateById(entity);
    }
}
