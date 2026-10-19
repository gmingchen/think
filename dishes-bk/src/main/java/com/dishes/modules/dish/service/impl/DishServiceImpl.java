package com.dishes.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.enums.ResultCodeEnum;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.common.utils.StringUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.category.entity.CategoryEntity;
import com.dishes.modules.category.mapper.CategoryMapper;
import com.dishes.modules.cookorder.entity.CookOrderDishEntity;
import com.dishes.modules.cookorder.entity.CookOrderEntity;
import com.dishes.modules.cookorder.mapper.CookOrderDishMapper;
import com.dishes.modules.cookorder.mapper.CookOrderMapper;
import com.dishes.modules.dish.convert.DishConvert;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.entity.DishFoodEntity;
import com.dishes.modules.dish.entity.DishSeasoningEntity;
import com.dishes.modules.dish.entity.DishStepEntity;
import com.dishes.modules.dish.mapper.DishMapper;
import com.dishes.modules.dish.model.dto.DishBatchUpdateCategoryDTO;
import com.dishes.modules.dish.model.dto.DishCollectDTO;
import com.dishes.modules.dish.model.dto.DishCreateDTO;
import com.dishes.modules.dish.model.dto.DishDeleteDTO;
import com.dishes.modules.dish.model.dto.DishFoodSaveUpdateDTO;
import com.dishes.modules.dish.model.dto.DishSeasoningSaveUpdateDTO;
import com.dishes.modules.dish.model.dto.DishStepSaveUpdateDTO;
import com.dishes.modules.dish.model.dto.DishShoppingListDTO;
import com.dishes.modules.dish.model.dto.DishSortDTO;
import com.dishes.modules.dish.model.dto.DishUpdateDTO;
import com.dishes.modules.dish.model.vo.DishFoodVO;
import com.dishes.modules.dish.model.vo.DishInfoVO;
import com.dishes.modules.dish.model.vo.DishSeasoningVO;
import com.dishes.modules.dish.model.vo.DishShoppingListVO;
import com.dishes.modules.dish.model.vo.DishShareVO;
import com.dishes.modules.dish.model.vo.DishVO;
import com.dishes.modules.dish.service.DishCollectService;
import com.dishes.modules.dish.service.DishFoodService;
import com.dishes.modules.dish.service.DishSeasoningService;
import com.dishes.modules.dish.service.DishService;
import com.dishes.modules.dish.service.DishStepService;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.service.FoodService;
import com.dishes.modules.kitchen.entity.KitchenEntity;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.service.KitchenService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.member.service.MemberService;
import com.dishes.modules.os.service.StorageService;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.service.SeasoningService;
import com.dishes.modules.user.entity.UserEntity;
import com.dishes.modules.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品
 * @author 好市民
 */
@Service("dishService")
public class DishServiceImpl extends ServiceImplX<DishMapper, DishEntity> implements DishService {

    @Autowired
    private KitchenService kitchenService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private KitchenUserService kitchenUserService;
    @Autowired
    private DishFoodService dishFoodService;
    @Autowired
    private DishSeasoningService dishSeasoningService;
    @Autowired
    private DishStepService dishStepService;
    @Autowired
    private StorageService storageService;
    @Autowired
    private UserService userService;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private FoodService foodService;
    @Autowired
    private SeasoningService seasoningService;
    @Autowired
    private DishCollectService dishCollectService;
    // 采购清单只跨模块取数据，直注 Mapper 避免与 CookOrderService（已依赖 DishService）形成循环依赖
    @Autowired
    private CookOrderMapper cookOrderMapper;
    @Autowired
    private CookOrderDishMapper cookOrderDishMapper;

    @Override
    public List<DishVO> list(Long kitchenId) {
        LambdaQueryWrapper<DishEntity> wrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, kitchenId)
                // 同分类内手动排序优先（sort 升序）；sort 相同（含存量全 0）再按更新时间倒序
                .orderByAsc(DishEntity::getSort)
                .orderByDesc(DishEntity::getUpdatedAt);
        List<DishEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, DishConvert.INSTANCE::convert);
    }

    @Override
    public List<DishVO> listByCategory(Long kitchenId, Long categoryId) {
        LambdaQueryWrapper<DishEntity> wrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, kitchenId);
        // categoryId 为 null 表示“未分类”组（与排序作用域一致）
        if (categoryId == null) {
            wrapper.isNull(DishEntity::getCategoryId);
        } else {
            wrapper.eq(DishEntity::getCategoryId, categoryId);
        }
        wrapper.orderByAsc(DishEntity::getSort).orderByDesc(DishEntity::getUpdatedAt);
        List<DishEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, DishConvert.INSTANCE::convert);
    }

    @Override
    public List<DishVO> best(Long kitchenId) {
        LambdaQueryWrapper<DishEntity> wrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, kitchenId)
                .gt(DishEntity::getCookCount, 0)
                .orderByDesc(DishEntity::getRating)
                .orderByDesc(DishEntity::getCookCount)
                .orderByDesc(DishEntity::getUpdatedAt)
                .last("LIMIT 5");
        List<DishEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, DishConvert.INSTANCE::convert);
    }

    @Override
    public DishInfoVO info(Long id) {
        DishEntity entity = validateExists(id);
        DishInfoVO vo = DishConvert.INSTANCE.convertInfo(entity);
        // 存量菜谱 images 列为 NULL 时归一为空数组，前端恒可得数组
        if (vo.getImages() == null) {
            vo.setImages(new ArrayList<>());
        }
        // 查询食材列表（带食材名称）
        vo.setFoods(dishFoodService.list(id));
        // 查询调料列表（带调料名称）
        vo.setSeasonings(dishSeasoningService.list(id));
        // 查询步骤列表
        vo.setSteps(dishStepService.list(id));
        return vo;
    }

    @Override
    public List<DishShoppingListVO> shoppingList(DishShoppingListDTO dto) {
        boolean hasDishIds = CollectionUtils.isNotEmpty(dto.getDishIds());
        boolean hasOrderIds = CollectionUtils.isNotEmpty(dto.getCookOrderIds());
        if (!hasDishIds && !hasOrderIds) {
            throw new RunException("菜谱列表与订单列表不能同时为空");
        }
        // 权限白名单：请求厨房（dishIds 路径边界）+ 已验权订单所属厨房，明细仅放行白名单内菜谱，防跨厨房猜 id
        // 成员厨房一次查出，后续均内存判断，替代逐单 checkMember 的 1+N 查询
        Long userId = SecurityUtils.getLoginUserId();
        Set<Long> memberKitchenIds = kitchenUserService.listKitchenIdsByUserId(userId);
        if (!memberKitchenIds.contains(dto.getKitchenId())) {
            throw new RunException("您不是该厨房成员");
        }
        Set<Long> allowedKitchenIds = new HashSet<>();
        allowedKitchenIds.add(dto.getKitchenId());
        // 份数汇总：dishIds 按重复出现次数计数，订单菜品数量累加；LinkedHashMap 保持先菜后单的出现顺序
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        // 菜→归属订单映射：同一菜可能出现在多个订单，前端勾选缓存按订单分片存储
        Map<Long, Set<Long>> dishOrderMap = new LinkedHashMap<>();
        if (hasDishIds) {
            for (Long dishId : dto.getDishIds()) {
                if (dishId != null) {
                    quantityMap.merge(dishId, 1, Integer::sum);
                }
            }
        }
        if (hasOrderIds) {
            List<Long> orderIds = dto.getCookOrderIds().stream()
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (!orderIds.isEmpty()) {
                // 内存比对预先查出的成员厨房集合（不存在的订单被 selectBatchIds 自然过滤）
                for (CookOrderEntity order : cookOrderMapper.selectBatchIds(orderIds)) {
                    if (!memberKitchenIds.contains(order.getKitchenId())) {
                        throw new RunException("您不是该厨房成员");
                    }
                    allowedKitchenIds.add(order.getKitchenId());
                }
                LambdaQueryWrapper<CookOrderDishEntity> odWrapper = new LambdaQueryWrapper<CookOrderDishEntity>()
                        .in(CookOrderDishEntity::getCookOrderId, orderIds)
                        .orderByAsc(CookOrderDishEntity::getId);
                for (CookOrderDishEntity od : cookOrderDishMapper.selectList(odWrapper)) {
                    quantityMap.merge(od.getDishId(), od.getQuantity() != null ? od.getQuantity() : 1, Integer::sum);
                    dishOrderMap.computeIfAbsent(od.getDishId(), k -> new LinkedHashSet<>()).add(od.getCookOrderId());
                }
            }
        }
        if (quantityMap.isEmpty()) {
            return Collections.emptyList();
        }
        // 已删除菜谱跳过（逻辑删除后 listByIds 自然查不到），非白名单厨房的菜谱静默丢弃（防跨厨房猜 id）
        Map<Long, DishEntity> dishMap = listByIds(new ArrayList<>(quantityMap.keySet())).stream()
                .collect(Collectors.toMap(DishEntity::getId, Function.identity()));
        List<Long> allowedDishIds = quantityMap.keySet().stream()
                .filter(id -> {
                    DishEntity dish = dishMap.get(id);
                    return dish != null && allowedKitchenIds.contains(dish.getKitchenId());
                })
                .collect(Collectors.toList());
        // 食材/调料各一次批量查，SQL 条数与菜数无关；已删除的食材/调料名称为 null，过滤掉避免前端渲染空行
        Map<Long, List<DishFoodVO>> foodsMap = dishFoodService.mapByDishIds(allowedDishIds);
        Map<Long, List<DishSeasoningVO>> seasoningsMap = dishSeasoningService.mapByDishIds(allowedDishIds);
        List<DishShoppingListVO> items = new ArrayList<>();
        for (Long dishId : allowedDishIds) {
            DishEntity dish = dishMap.get(dishId);
            items.add(new DishShoppingListVO()
                    .setDishId(dish.getId())
                    .setDishName(dish.getName())
                    .setDishCover(dish.getCover())
                    .setQuantity(quantityMap.get(dishId))
                    .setCookOrderIds(new ArrayList<>(dishOrderMap.getOrDefault(dishId, Collections.emptySet())))
                    .setFoods(foodsMap.getOrDefault(dishId, Collections.emptyList()).stream()
                            .filter(f -> f.getFoodName() != null)
                            .collect(Collectors.toList()))
                    .setSeasonings(seasoningsMap.getOrDefault(dishId, Collections.emptyList()).stream()
                            .filter(s -> s.getName() != null)
                            .collect(Collectors.toList())));
        }
        return items;
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public Long create(DishCreateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        kitchenUserService.checkPermission(dto.getKitchenId());
        // 检查菜品数是否已达上限
        KitchenEntity kitchen = kitchenService.validateExists(dto.getKitchenId());
        LambdaQueryWrapper<DishEntity> wrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, dto.getKitchenId());
        // 畅厨会员不受容量闸门约束，但**仅在自己创建的厨房里**短路（docs/v12 §6.2 / §1.4 决策 1）；
        // 判定读登录态快照，零额外查询，owner_id 已在 kitchen 行上
        boolean unlimited = memberService.hasUnlimitedCapacity(userId)
                && kitchen.getOwnerId().equals(userId);
        if (!unlimited && count(wrapper) >= kitchen.getMaxDishCount()) {
            throw new RunException(ResultCodeEnum.LIMIT_REACHED.getCode(),
                    "菜谱数已达上限（" + kitchen.getMaxDishCount() + "道），删除部分旧内容后可继续添加");
        }
        // 插入菜品
        DishEntity dishEntity = DishConvert.INSTANCE.convertEntity(dto)
                .setCreatorId(userId)
                .setCookCount(0)
                // 详情图：未传则存空数组（列可空，统一以 [] 表达"无详情图"）
                .setImages(dto.getImages() != null ? dto.getImages() : new ArrayList<>())
                // 新建菜谱排到所属分类（categoryId 为空则“未分类”组）末尾
                .setSort(getNextSort(dto.getKitchenId(), dto.getCategoryId()));
        baseMapper.insert(dishEntity);
        // 插入子实体
        Long dishId = dishEntity.getId();
        DishFoodSaveUpdateDTO foodSaveDTO = new DishFoodSaveUpdateDTO()
                .setId(dishId).setList(dto.getFoods());
        dishFoodService.create(foodSaveDTO);
        DishSeasoningSaveUpdateDTO seasoningSaveDTO = new DishSeasoningSaveUpdateDTO()
                .setId(dishId).setList(dto.getSeasonings());
        dishSeasoningService.create(seasoningSaveDTO);
        DishStepSaveUpdateDTO stepSaveDTO = new DishStepSaveUpdateDTO()
                .setId(dishId).setList(dto.getSteps());
        dishStepService.create(stepSaveDTO);
        return dishId;
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void update(DishUpdateDTO dto) {
        check(dto.getId());
        DishEntity oldEntity = validateExists(dto.getId());
        // 对比旧封面，封面变更时删除旧文件
        if (dto.getCover() != null) {
            if (oldEntity.getCover() != null && !oldEntity.getCover().equals(dto.getCover())) {
                storageService.deleteByUrl(oldEntity.getCover());
            }
        }
        // 对比旧详情图，删除本次编辑中被移除的文件（新集合未包含的旧 URL）
        // 注：dto.images 为 null 表示未提交该字段（updateById 会跳过、保留原值），此时不做任何清理
        if (dto.getImages() != null && oldEntity.getImages() != null) {
            for (String url : oldEntity.getImages()) {
                if (url != null && !dto.getImages().contains(url)) {
                    storageService.deleteByUrl(url);
                }
            }
        }
        // 更新菜品基础信息
        DishEntity entity = DishConvert.INSTANCE.convertEntity(dto);
        // 分类发生变更时，把排序重置到新分类（或“未分类”组）末尾
        // 注：updateById 忽略 null 字段，dto.categoryId 为空即“未改分类”，保持原排序不动
        if (dto.getCategoryId() != null && !dto.getCategoryId().equals(oldEntity.getCategoryId())) {
            entity.setSort(getNextSort(oldEntity.getKitchenId(), dto.getCategoryId()));
        }
        baseMapper.updateById(entity);
        Long dishId = dto.getId();
        // 全量替换子实体
        if (dto.getFoods() != null) {
            DishFoodSaveUpdateDTO foodSaveDTO = new DishFoodSaveUpdateDTO()
                    .setId(dishId).setList(dto.getFoods());
            dishFoodService.update(foodSaveDTO);
        }
        if (dto.getSeasonings() != null) {
            DishSeasoningSaveUpdateDTO seasoningSaveDTO = new DishSeasoningSaveUpdateDTO()
                    .setId(dishId).setList(dto.getSeasonings());
            dishSeasoningService.update(seasoningSaveDTO);
        }
        if (dto.getSteps() != null) {
            DishStepSaveUpdateDTO stepSaveDTO = new DishStepSaveUpdateDTO()
                    .setId(dishId).setList(dto.getSteps());
            dishStepService.update(stepSaveDTO);
        }
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void delete(DishDeleteDTO dto) {
        List<Long> ids = dto.getIds();
        for (Long id : ids) {
            check(id);
        }
        // 删除关联封面/详情图文件
        for (Long id : ids) {
            DishEntity entity = validateExists(id);
            if (entity.getCover() != null) {
                storageService.deleteByUrl(entity.getCover());
            }
            if (entity.getImages() != null) {
                for (String url : entity.getImages()) {
                    if (url != null) {
                        storageService.deleteByUrl(url);
                    }
                }
            }
        }
        // 删除子实体（含回补使用次数）
        for (Long id : ids) {
            dishFoodService.deleteByDishId(id);
            dishSeasoningService.deleteByDishId(id);
            dishStepService.deleteByDishId(id);
        }
        // 批量删除菜品
        baseMapper.deleteBatchIds(ids);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void batchUpdateCategory(DishBatchUpdateCategoryDTO dto) {
        List<Long> ids = dto.getIds();
        Long categoryId = dto.getCategoryId();
        // 校验权限
        for (Long id : ids) {
            check(id);
        }
        // 批量移动到目标分类末尾：按传入顺序依次追加，保持被选中菜谱的相对顺序
        // 批量改分类限定同一厨房（菜谱页为单厨房操作），取首条菜谱的厨房ID计算目标分类末尾排序值
        Long kitchenId = validateExists(ids.get(0)).getKitchenId();
        int baseSort = getNextSort(kitchenId, categoryId);
        for (int i = 0; i < ids.size(); i++) {
            LambdaUpdateWrapper<DishEntity> wrapper = new LambdaUpdateWrapper<DishEntity>()
                    .eq(DishEntity::getId, ids.get(i))
                    .set(DishEntity::getCategoryId, categoryId)
                    .set(DishEntity::getSort, baseSort + i);
            baseMapper.update(null, wrapper);
        }
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void sort(DishSortDTO dto) {
        kitchenUserService.checkPermission(dto.getKitchenId());
        List<Long> ids = dto.getIds();
        for (int i = 0; i < ids.size(); i++) {
            LambdaUpdateWrapper<DishEntity> wrapper = new LambdaUpdateWrapper<DishEntity>()
                    .eq(DishEntity::getId, ids.get(i))
                    .eq(DishEntity::getKitchenId, dto.getKitchenId());
            // 限定在目标分类（categoryId 为空即“未分类”组）内落库，防跨分类猜 id
            if (dto.getCategoryId() == null) {
                wrapper.isNull(DishEntity::getCategoryId);
            } else {
                wrapper.eq(DishEntity::getCategoryId, dto.getCategoryId());
            }
            wrapper.set(DishEntity::getSort, i);
            baseMapper.update(null, wrapper);
        }
    }

    // ==================== 权限校验 ====================

    private void check(Long id) {
        DishEntity entity = validateExists(id);
        kitchenUserService.checkPermission(entity.getKitchenId());
    }

    /**
     * 取指定厨房、指定分类内下一个排序值（末尾追加）。
     * categoryId 为 null 表示“未分类”组；组内为空时从 0 起，否则取当前 max+1。
     */
    private int getNextSort(Long kitchenId, Long categoryId) {
        LambdaQueryWrapper<DishEntity> wrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, kitchenId);
        if (categoryId == null) {
            wrapper.isNull(DishEntity::getCategoryId);
        } else {
            wrapper.eq(DishEntity::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(DishEntity::getSort).last("LIMIT 1");
        DishEntity last = getOne(wrapper);
        return (last != null && last.getSort() != null) ? last.getSort() + 1 : 0;
    }

    @Override
    public void incrementCookCount(List<Long> dishIds) {
        if (CollectionUtils.isEmpty(dishIds)) {
            return;
        }
        for (Long dishId : dishIds) {
            LambdaUpdateWrapper<DishEntity> wrapper = new LambdaUpdateWrapper<DishEntity>()
                    .eq(DishEntity::getId, dishId)
                    .setSql("cook_count = cook_count + 1");
            baseMapper.update(null, wrapper);
        }
    }

    @Override
    public DishShareVO shareInfo(Long id) {
        DishEntity entity = validateExists(id);
        DishShareVO vo = new DishShareVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setCover(entity.getCover());
        vo.setRating(entity.getRating());
        vo.setCookCount(entity.getCookCount());
        vo.setCollectCount(entity.getCollectCount() != null ? entity.getCollectCount() : 0);
        vo.setRemark(entity.getRemark());
        // 食材、调料、步骤
        vo.setFoods(dishFoodService.list(id));
        vo.setSeasonings(dishSeasoningService.list(id));
        vo.setSteps(dishStepService.list(id));
        // 分类名称
        if (entity.getCategoryId() != null) {
            CategoryEntity category = categoryMapper.selectById(entity.getCategoryId());
            vo.setCategoryName(category != null ? category.getName() : null);
        }
        // 分享者信息
        UserEntity creator = userService.getById(entity.getCreatorId());
        KitchenEntity kitchen = kitchenService.getById(entity.getKitchenId());
        DishShareVO.SharerInfo sharer = new DishShareVO.SharerInfo()
                .setNickname(creator != null ? creator.getNickname() : "")
                .setAvatar(creator != null ? creator.getAvatar() : "")
                .setKitchenName(kitchen != null ? kitchen.getName() : "");
        // 分享者在该厨房的角色（已退出厨房则取不到，前端不展示角色标签）
        LambdaQueryWrapper<KitchenUserEntity> sharerRoleWrapper = new LambdaQueryWrapper<KitchenUserEntity>()
                .eq(KitchenUserEntity::getKitchenId, entity.getKitchenId())
                .eq(KitchenUserEntity::getUserId, entity.getCreatorId())
                .last("LIMIT 1");
        KitchenUserEntity sharerKitchenUser = kitchenUserService.getOne(sharerRoleWrapper);
        sharer.setRole(sharerKitchenUser != null ? sharerKitchenUser.getRole() : null);
        vo.setSharer(sharer);
        // 分享者厨房菜谱总数
        LambdaQueryWrapper<DishEntity> countWrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, entity.getKitchenId());
        vo.setDishCount((int) count(countWrapper));
        // 已被收藏到的厨房，供前端标记「已收藏」
        vo.setCollectedKitchens(dishCollectService.listCollectedKitchenIds(id));
        return vo;
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void collect(DishCollectDTO dto) {
        // 1. 验证源菜谱存在
        DishEntity sourceDish = validateExists(dto.getDishId());
        Long userId = SecurityUtils.getLoginUserId();
        Long targetKitchenId = dto.getKitchenId();
        // 2. 源菜谱属于目标厨房时，本就是本厨房的菜，无需收藏
        if (sourceDish.getKitchenId().equals(targetKitchenId)) {
            throw new RunException("这道菜已是当前厨房的菜谱，无需收藏");
        }
        // 3. 校验目标厨房权限（厨师长/管厨）
        kitchenUserService.checkPermission(targetKitchenId);
        // 4. 去重：同一菜谱不能重复收藏进同一厨房（并发由唯一索引 uk_dish_kitchen 兜底）
        if (dishCollectService.collected(dto.getDishId(), targetKitchenId)) {
            throw new RunException("这道菜已经收藏在该厨房中");
        }
        // 5. 检查目标厨房菜谱数是否达上限
        KitchenEntity targetKitchen = kitchenService.validateExists(targetKitchenId);
        LambdaQueryWrapper<DishEntity> countWrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getKitchenId, targetKitchenId);
        // 收藏入库与 AI 采纳写的是**当前活跃厨房**，所以这一处同样必须带 owner 比较，
        // 否则管厨买畅厨后就能用「收藏」突破别人厨房的上限（§6.2 / §12.2）
        boolean unlimited = memberService.hasUnlimitedCapacity(userId)
                && targetKitchen.getOwnerId().equals(userId);
        if (!unlimited && count(countWrapper) >= targetKitchen.getMaxDishCount()) {
            throw new RunException(ResultCodeEnum.LIMIT_REACHED.getCode(),
                    "菜谱数已达上限（" + targetKitchen.getMaxDishCount() + "道），删除部分旧内容后可继续添加");
        }
        // 6. 创建新菜谱（不带封面、不带走做过次数/评分/收藏数）
        DishEntity newDish = new DishEntity()
                .setKitchenId(targetKitchenId)
                .setCreatorId(userId)
                .setName(sourceDish.getName())
                .setRemark(sourceDish.getRemark())
                .setCookCount(0)
                .setCollectCount(0)
                // 收藏而来的菜谱无分类，排到目标厨房“未分类”组末尾
                .setSort(getNextSort(targetKitchenId, null));
        baseMapper.insert(newDish);
        Long newDishId = newDish.getId();
        // 7. 处理食材：按名称匹配目标厨房（已抽为公共方法，与 AI 荐菜采纳同口径），已有复用，没有新增
        LambdaQueryWrapper<DishFoodEntity> sourceFoodWrapper = new LambdaQueryWrapper<DishFoodEntity>()
                .eq(DishFoodEntity::getDishId, dto.getDishId())
                .orderByAsc(DishFoodEntity::getSort);
        List<DishFoodEntity> sourceFoods = dishFoodService.list(sourceFoodWrapper);
        if (CollectionUtils.isNotEmpty(sourceFoods)) {
            // 查询源食材名称
            List<Long> sourceFoodIds = CollectionUtils.mapList(sourceFoods, DishFoodEntity::getFoodId);
            Map<Long, FoodEntity> sourceFoodMap = foodService.listByIds(sourceFoodIds).stream()
                    .collect(Collectors.toMap(FoodEntity::getId, f -> f));
            // 按源食材行逐个匹配/新建（重复行各计一次使用，与原行为一致）
            List<String> foodNames = sourceFoods.stream()
                    .map(sf -> sourceFoodMap.get(sf.getFoodId()))
                    .filter(Objects::nonNull)
                    .map(FoodEntity::getName)
                    .collect(Collectors.toList());
            Map<String, Long> matchedFoodIds = foodService.matchOrCreateByNames(targetKitchenId, foodNames, userId);
            List<DishFoodEntity> newDishFoods = new ArrayList<>();
            for (DishFoodEntity sf : sourceFoods) {
                FoodEntity sourceFood = sourceFoodMap.get(sf.getFoodId());
                if (sourceFood == null) continue;
                Long targetFoodId = matchedFoodIds.get(StringUtils.normalizeName(sourceFood.getName()));
                if (targetFoodId == null) continue;
                newDishFoods.add(new DishFoodEntity()
                        .setDishId(newDishId)
                        .setFoodId(targetFoodId)
                        .setAmount(sf.getAmount())
                        .setUnit(sf.getUnit())
                        .setProcess(sf.getProcess())
                        .setIsOptional(sf.getIsOptional())
                        .setSort(sf.getSort()));
            }
            if (CollectionUtils.isNotEmpty(newDishFoods)) {
                dishFoodService.saveBatch(newDishFoods);
            }
        }
        // 8. 处理调料：同食材逻辑（复用公共匹配方法）
        LambdaQueryWrapper<DishSeasoningEntity> sourceSeasoningWrapper = new LambdaQueryWrapper<DishSeasoningEntity>()
                .eq(DishSeasoningEntity::getDishId, dto.getDishId())
                .orderByAsc(DishSeasoningEntity::getSort);
        List<DishSeasoningEntity> sourceSeasonings = dishSeasoningService.list(sourceSeasoningWrapper);
        if (CollectionUtils.isNotEmpty(sourceSeasonings)) {
            List<Long> sourceSeasoningIds = CollectionUtils.mapList(sourceSeasonings, DishSeasoningEntity::getSeasoningId);
            Map<Long, SeasoningEntity> sourceSeasoningMap = seasoningService.listByIds(sourceSeasoningIds).stream()
                    .collect(Collectors.toMap(SeasoningEntity::getId, s -> s));
            List<String> seasoningNames = sourceSeasonings.stream()
                    .map(ss -> sourceSeasoningMap.get(ss.getSeasoningId()))
                    .filter(Objects::nonNull)
                    .map(SeasoningEntity::getName)
                    .collect(Collectors.toList());
            Map<String, Long> matchedSeasoningIds = seasoningService.matchOrCreateByNames(targetKitchenId, seasoningNames, userId);
            List<DishSeasoningEntity> newDishSeasonings = new ArrayList<>();
            for (DishSeasoningEntity ss : sourceSeasonings) {
                SeasoningEntity sourceSeasoning = sourceSeasoningMap.get(ss.getSeasoningId());
                if (sourceSeasoning == null) continue;
                Long targetSeasoningId = matchedSeasoningIds.get(StringUtils.normalizeName(sourceSeasoning.getName()));
                if (targetSeasoningId == null) continue;
                newDishSeasonings.add(new DishSeasoningEntity()
                        .setDishId(newDishId)
                        .setSeasoningId(targetSeasoningId)
                        .setAmount(ss.getAmount())
                        .setUnit(ss.getUnit())
                        .setIsOptional(ss.getIsOptional())
                        .setSort(ss.getSort()));
            }
            if (CollectionUtils.isNotEmpty(newDishSeasonings)) {
                dishSeasoningService.saveBatch(newDishSeasonings);
            }
        }
        // 9. 处理步骤：直接复制（不带图片）
        LambdaQueryWrapper<DishStepEntity> sourceStepWrapper = new LambdaQueryWrapper<DishStepEntity>()
                .eq(DishStepEntity::getDishId, dto.getDishId())
                .orderByAsc(DishStepEntity::getSort);
        List<DishStepEntity> sourceSteps = dishStepService.list(sourceStepWrapper);
        if (CollectionUtils.isNotEmpty(sourceSteps)) {
            List<DishStepEntity> newSteps = new ArrayList<>();
            for (DishStepEntity ss : sourceSteps) {
                newSteps.add(new DishStepEntity()
                        .setDishId(newDishId)
                        .setDescription(ss.getDescription())
                        .setSort(ss.getSort()));
            }
            dishStepService.saveBatch(newSteps);
        }
        // 10. 记录收藏关系（幂等去重的依据）
        dishCollectService.record(dto.getDishId(), targetKitchenId, newDishId);
        // 11. 源菜谱 collect_count + 1（步骤2 已排除同厨房，此处必为跨厨房收藏）
        LambdaUpdateWrapper<DishEntity> collectWrapper = new LambdaUpdateWrapper<DishEntity>()
                .eq(DishEntity::getId, dto.getDishId())
                .setSql("collect_count = IFNULL(collect_count, 0) + 1");
        baseMapper.update(null, collectWrapper);
    }
}