package com.dishes.modules.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.enums.KitchenExpandFieldEnum;
import com.dishes.common.enums.KitchenRoleEnum;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.cookorder.entity.CookOrderDishEntity;
import com.dishes.modules.cookorder.entity.CookOrderEntity;
import com.dishes.modules.cookorder.mapper.CookOrderDishMapper;
import com.dishes.modules.cookorder.mapper.CookOrderMapper;
import com.dishes.modules.category.entity.CategoryEntity;
import com.dishes.modules.category.mapper.CategoryMapper;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.mapper.DishMapper;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.mapper.FoodMapper;
import com.dishes.modules.kitchen.convert.KitchenConvert;
import com.dishes.modules.kitchen.entity.KitchenEntity;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.mapper.KitchenMapper;
import com.dishes.modules.kitchen.model.dto.KitchenCreateDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUpdateDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserCreateDTO;
import com.dishes.modules.kitchen.model.vo.KitchenStatsVO;
import com.dishes.modules.kitchen.model.vo.KitchenVO;
import com.dishes.modules.kitchen.service.KitchenService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.os.service.StorageService;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.mapper.SeasoningMapper;
import com.dishes.modules.auth.model.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 厨房
 * @author 好市民
 */
@Service("kitchenService")
public class KitchenServiceImpl extends ServiceImplX<KitchenMapper, KitchenEntity> implements KitchenService {

    /**
     * 默认最大分类数
     */
    private static final int DEFAULT_MAX_CATEGORY_COUNT = 10;
    /**
     * 默认最大菜品数
     */
    private static final int DEFAULT_MAX_DISH_COUNT = 50;
    /**
     * 默认最大成员数
     */
    private static final int DEFAULT_MAX_MEMBER_COUNT = 5;
    /**
     * 畅厨档位（= {@code MemberPlanEntity.TIER_PRO}）：只有它带无限容量权益，轻享不免限数（docs/v12 §1.2）。
     * 这里写常数而不引进 member 模块的常量，是为了不让 kitchen 反向依赖 member（§9.2：不注 MemberService）
     */
    private static final int TIER_PRO = 2;

    @Autowired
    private KitchenUserService kitchenUserService;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private CookOrderMapper cookOrderMapper;
    @Autowired
    private CookOrderDishMapper cookOrderDishMapper;
    @Autowired
    private FoodMapper foodMapper;
    @Autowired
    private SeasoningMapper seasoningMapper;
    @Autowired
    private StorageService storageService;

    @Override
    public List<KitchenVO> getList() {
        Long userId = SecurityUtils.getLoginUserId();
        // 查询当前用户所属的厨房
        LambdaQueryWrapper<KitchenUserEntity> memberWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getUserId, userId);
        List<KitchenUserEntity> memberships = kitchenUserService.list(memberWrapper);
        if (CollectionUtils.isEmpty(memberships)) {
            return Collections.emptyList();
        }
        // 构建 kitchenId -> role 映射
        Map<Long, Integer> roleMap = new HashMap<>();
        List<Long> kitchenIds = new ArrayList<>();
        for (KitchenUserEntity membership : memberships) {
            kitchenIds.add(membership.getKitchenId());
            roleMap.put(membership.getKitchenId(), membership.getRole());
        }
        // 查询厨房列表
        List<KitchenEntity> entities = listByIds(kitchenIds);
        int proTier = proMemberTier(userId);
        return CollectionUtils.mapList(entities, entity -> {
            KitchenVO vo = KitchenConvert.INSTANCE.convert(entity);
            vo.setRole(roleMap.get(entity.getId()));
            markCapacityFlags(vo, entity, userId, proTier);
            // 查询成员数
            LambdaQueryWrapper<KitchenUserEntity> countWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                    .eq(KitchenUserEntity::getKitchenId, entity.getId());
            vo.setMemberCount((int) kitchenUserService.count(countWrapper));
            // 查询菜品数
            LambdaQueryWrapper<DishEntity> dishCountWrapper = new LambdaQueryWrapper<DishEntity>()
                    .eq(DishEntity::getKitchenId, entity.getId());
            vo.setDishCount(dishMapper.selectCount(dishCountWrapper).intValue());
            return vo;
        });
    }

    @Override
    public KitchenVO info(Long kitchenId) {
        Long userId = SecurityUtils.getLoginUserId();
        KitchenEntity entity = validateExists(kitchenId);
        KitchenVO vo = KitchenConvert.INSTANCE.convert(entity);
        // 查询当前用户角色
        LambdaQueryWrapper<KitchenUserEntity> roleWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, kitchenId)
                .eq(KitchenUserEntity::getUserId, userId);
        KitchenUserEntity membership = kitchenUserService.getOne(roleWrapper);
        if (membership != null) {
            vo.setRole(membership.getRole());
        }
        // 查询成员数
        LambdaQueryWrapper<KitchenUserEntity> memberWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, kitchenId);
        vo.setMemberCount((int) kitchenUserService.count(memberWrapper));
        // 查询分类数
        LambdaQueryWrapper<CategoryEntity> categoryWrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, kitchenId);
        vo.setCategoryCount(categoryMapper.selectCount(categoryWrapper).intValue());
        // 查询菜谱数
        LambdaQueryWrapper<DishEntity> dishWrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, kitchenId);
        vo.setDishCount(dishMapper.selectCount(dishWrapper).intValue());
        markCapacityFlags(vo, entity, userId, proMemberTier(userId));
        return vo;
    }

    /**
     * 当前登录人的畅厨档位（读时判定，不落库、不改数据）
     * <p>只取 Redis 里的登录快照，不为判定去查 member 模块的表（§9.2：kitchen 不注 MemberService）；
     * 快照不是本人或取不到时一律返回 0，宁可少给权益也不能多给
     * @param userId 当前登录用户ID
     * @return 有效档位，0 表示非会员或已过期
     */
    private int proMemberTier(Long userId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null || userId == null || !userId.equals(user.getId())) {
            return 0;
        }
        return user.memberTierAt(LocalDateTime.now());
    }

    /**
     * 给厨房VO打「是否本人创建」「是否不限容量」两个标记，供前端隐藏扩容入口与上限文案
     * <p>unlimitedCapacity 必须同时满足 owner 与畅厨档位，只判档位的话，
     * 成员在别人的厨房里也会看到「不限」，而服务端校验仍按 owner 拦（§6.2、§4 入口 4）
     * @param vo 待打标的厨房视图
     * @param entity 厨房实体
     * @param userId 当前登录用户ID
     * @param proTier 当前登录人的有效档位
     */
    private void markCapacityFlags(KitchenVO vo, KitchenEntity entity, Long userId, int proTier) {
        boolean owner = entity.getOwnerId() != null && entity.getOwnerId().equals(userId);
        vo.setIsOwner(owner);
        vo.setUnlimitedCapacity(owner && proTier >= TIER_PRO);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public Long create(KitchenCreateDTO dto) {
        Long userId = dto.getUserId();
        // 判断当前用户是否已拥有厨房
        LambdaQueryWrapper<KitchenEntity> wrapper = new LambdaQueryWrapper<KitchenEntity>()
                .eq(KitchenEntity::getOwnerId, userId);
        if (count(wrapper) > 0) {
            throw new RunException("每个用户只能创建一个厨房");
        }
        KitchenEntity entity = new KitchenEntity()
                .setName(dto.getName())
                .setCover(dto.getCover())
                .setMaxCategoryCount(DEFAULT_MAX_CATEGORY_COUNT)
                .setMaxDishCount(DEFAULT_MAX_DISH_COUNT)
                .setMaxMemberCount(DEFAULT_MAX_MEMBER_COUNT)
                .setOwnerId(userId);
        baseMapper.insert(entity);
        // 创建者以厨师长角色加入厨房
        KitchenUserCreateDTO memberDTO = new KitchenUserCreateDTO()
                .setKitchenId(entity.getId())
                .setUserId(userId)
                .setRole(KitchenRoleEnum.OWNER.getCode());
        kitchenUserService.create(memberDTO);
        return entity.getId();
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public Long createDefault(Long userId) {
        KitchenCreateDTO dto = new KitchenCreateDTO()
                .setUserId(userId)
                .setName("我的厨房");
        return create(dto);
    }

    @Override
    public void update(KitchenUpdateDTO dto) {
        KitchenEntity entity = validateExists(dto.getId());
        kitchenUserService.checkPermission(dto.getId());
        // 对比旧封面，封面变更时删除旧文件
        if (dto.getCover() != null && entity.getCover() != null && !entity.getCover().equals(dto.getCover())) {
            storageService.deleteByUrl(entity.getCover());
        }
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getCover() != null) {
            entity.setCover(dto.getCover());
        }
        baseMapper.updateById(entity);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void delete(Long id) {
        Long userId = SecurityUtils.getLoginUserId();
        KitchenEntity entity = validateExists(id);
        // 判断操作人是否为厨师长
        if (!entity.getOwnerId().equals(userId)) {
            throw new RunException("只有厨师长才能删除厨房");
        }
        // 删除厨房封面文件
        if (entity.getCover() != null) {
            storageService.deleteByUrl(entity.getCover());
        }
        // 删除厨房成员关联
        LambdaQueryWrapper<KitchenUserEntity> wrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, id);
        kitchenUserService.remove(wrapper);
        baseMapper.deleteById(id);
    }

    @Override
    public KitchenStatsVO getStats(Long kitchenId) {
        // 查询分类数
        LambdaQueryWrapper<CategoryEntity> categoryWrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, kitchenId);
        int categoryCount = categoryMapper.selectCount(categoryWrapper).intValue();
        // 查询菜谱数
        LambdaQueryWrapper<DishEntity> dishWrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, kitchenId);
        int dishCount = dishMapper.selectCount(dishWrapper).intValue();
        // 查询下厨次数
        LambdaQueryWrapper<CookOrderEntity> orderWrapper = new LambdaQueryWrapper<CookOrderEntity>()
                .eq(CookOrderEntity::getKitchenId, kitchenId);
        int cookOrderCount = cookOrderMapper.selectCount(orderWrapper).intValue();
        // 查询盛盘菜品数（通过下厨记录关联）
        LambdaQueryWrapper<CookOrderDishEntity> orderDishWrapper = new LambdaQueryWrapper<CookOrderDishEntity>()
                .inSql(CookOrderDishEntity::getCookOrderId,
                        "SELECT id FROM cook_order WHERE kitchen_id = " + kitchenId + " AND deleted = 0");
        int cookOrderDishCount = cookOrderDishMapper.selectCount(orderDishWrapper).intValue();
        // 查询厨房成员数
        LambdaQueryWrapper<KitchenUserEntity> memberWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, kitchenId);
        int memberCount = (int) kitchenUserService.count(memberWrapper);
        // 查询食材数（厨房维度）
        LambdaQueryWrapper<FoodEntity> foodWrapper = new LambdaQueryWrapper<FoodEntity>()
                .eq(FoodEntity::getKitchenId, kitchenId);
        int foodCount = foodMapper.selectCount(foodWrapper).intValue();
        // 查询调料数（厨房维度）
        LambdaQueryWrapper<SeasoningEntity> seasoningWrapper = new LambdaQueryWrapper<SeasoningEntity>()
                .eq(SeasoningEntity::getKitchenId, kitchenId);
        int seasoningCount = seasoningMapper.selectCount(seasoningWrapper).intValue();
        return new KitchenStatsVO()
                .setCategoryCount(categoryCount)
                .setDishCount(dishCount)
                .setCookOrderCount(cookOrderCount)
                .setCookOrderDishCount(cookOrderDishCount)
                .setFoodCount(foodCount)
                .setSeasoningCount(seasoningCount)
                .setMemberCount(memberCount);
    }

    @Override
    public KitchenVO expandDish(Long kitchenId) {
        return expandSlot(kitchenId, KitchenExpandFieldEnum.DISH);
    }

    @Override
    public KitchenVO expandCategory(Long kitchenId) {
        return expandSlot(kitchenId, KitchenExpandFieldEnum.CATEGORY);
    }

    /**
     * 扩容公用流程：权限校验 + 原子自增 + 返回刷新后的厨房信息
     * <p>菜谱、分类两个接口均委托此方法，仅传入不同的扩容字段枚举
     * @param kitchenId 厨房ID
     * @param field 扩容字段（对应 kitchen 表的可自增列）
     * @return 扩容后的厨房信息
     */
    private KitchenVO expandSlot(Long kitchenId, KitchenExpandFieldEnum field) {
        // 仅厨师长/管厨可扩容（食客在此抛「您没有操作权限」）
        kitchenUserService.checkPermission(kitchenId);
        validateExists(kitchenId);
        // 原子自增，不读后写，避免并发丢增；列名来自固定枚举，无拼接注入风险
        String column = field.getColumn();
        LambdaUpdateWrapper<KitchenEntity> wrapper = new LambdaUpdateWrapper<KitchenEntity>()
                .eq(KitchenEntity::getId, kitchenId)
                .setSql(column + " = " + column + " + 1");
        baseMapper.update(null, wrapper);
        // 返回刷新后的厨房信息，前端据此更新 maxDishCount / maxCategoryCount
        return info(kitchenId);
    }

}