package com.dishes.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.enums.ResultCodeEnum;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.category.convert.CategoryConvert;
import com.dishes.modules.category.entity.CategoryEntity;
import com.dishes.modules.category.mapper.CategoryMapper;
import com.dishes.modules.category.model.dto.CategoryCreateDTO;
import com.dishes.modules.category.model.dto.CategoryDeleteDTO;
import com.dishes.modules.category.model.dto.CategorySortDTO;
import com.dishes.modules.category.model.dto.CategoryUpdateDTO;
import com.dishes.modules.category.model.vo.CategoryVO;
import com.dishes.modules.category.service.CategoryService;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.service.DishService;
import com.dishes.modules.kitchen.entity.KitchenEntity;
import com.dishes.modules.kitchen.service.KitchenService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import com.dishes.modules.member.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 分类
 * @author 好市民
 */
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImplX<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private KitchenService kitchenService;
    @Autowired
    private KitchenUserService kitchenUserService;
    @Autowired
    private DishService dishService;
    @Autowired
    private MemberService memberService;

    @Override
    public List<CategoryVO> list(Long kitchenId) {
        LambdaQueryWrapper<CategoryEntity> wrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, kitchenId)
                .orderByAsc(CategoryEntity::getSort);
        List<CategoryEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, entity -> {
            CategoryVO vo = CategoryConvert.INSTANCE.convert(entity);
            return vo;
        });
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public Long create(CategoryCreateDTO dto) {
        kitchenUserService.checkPermission(dto.getKitchenId());
        // 检查分类数是否已达上限
        KitchenEntity kitchen = kitchenService.validateExists(dto.getKitchenId());
        LambdaQueryWrapper<CategoryEntity> countWrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, dto.getKitchenId());
        // 与菜谱那一处同构：畅厨会员只在**自己创建的厨房**里不受限（docs/v12 §6.2），
        // 少了 owner 比较就等于引导管厨花钱解除别人厨房的限制（§4 入口 4 同坑）
        Long userId = SecurityUtils.getLoginUserId();
        boolean unlimited = memberService.hasUnlimitedCapacity(userId)
                && kitchen.getOwnerId().equals(userId);
        if (!unlimited && count(countWrapper) >= kitchen.getMaxCategoryCount()) {
            throw new RunException(ResultCodeEnum.LIMIT_REACHED.getCode(),
                    "分类数已达上限（" + kitchen.getMaxCategoryCount() + "个），删除部分旧内容后可继续添加");
        }
        // 检查同厨房下分类名是否重复
        LambdaQueryWrapper<CategoryEntity> nameWrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, dto.getKitchenId())
                .eq(CategoryEntity::getName, dto.getName());
        if (count(nameWrapper) > 0) {
            throw new RunException("分类名称已存在");
        }
        // 获取当前最大排序值
        LambdaQueryWrapper<CategoryEntity> sortWrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, dto.getKitchenId())
                .orderByDesc(CategoryEntity::getSort)
                .last("LIMIT 1");
        CategoryEntity lastCategory = getOne(sortWrapper);
        int sort = (lastCategory != null && lastCategory.getSort() != null) ? lastCategory.getSort() + 1 : 0;
        CategoryEntity entity = new CategoryEntity()
                .setKitchenId(dto.getKitchenId())
                .setName(dto.getName())
                .setSort(sort);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(CategoryUpdateDTO dto) {
        CategoryEntity entity = validateExists(dto.getId());
        kitchenUserService.checkPermission(entity.getKitchenId());
        // 检查同厨房下分类名是否重复（排除自身）
        LambdaQueryWrapper<CategoryEntity> wrapper = new LambdaQueryWrapper<CategoryEntity>()
                .eq(CategoryEntity::getKitchenId, entity.getKitchenId())
                .eq(CategoryEntity::getName, dto.getName())
                .ne(CategoryEntity::getId, dto.getId());
        if (count(wrapper) > 0) {
            throw new RunException("分类名称已存在");
        }
        entity.setName(dto.getName());
        baseMapper.updateById(entity);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void delete(CategoryDeleteDTO dto) {
        CategoryEntity entity = validateExists(dto.getId());
        kitchenUserService.checkPermission(entity.getKitchenId());
        // 检查该分类下是否有菜品，有则迁移或置空
        LambdaQueryWrapper<DishEntity> dishWrapper = new LambdaQueryWrapper<DishEntity>()
                .eq(DishEntity::getCategoryId, dto.getId());
        long dishCount = dishService.count(dishWrapper);
        if (dishCount > 0) {
            Long targetCategoryId = dto.getTargetCategoryId();
            if (targetCategoryId != null) {
                CategoryEntity targetCategory = validateExists(targetCategoryId);
                if (!targetCategory.getKitchenId().equals(entity.getKitchenId())) {
                    throw new RunException("目标分类不属于同一厨房");
                }
            }
            LambdaUpdateWrapper<DishEntity> moveWrapper = new LambdaUpdateWrapper<DishEntity>()
                    .eq(DishEntity::getCategoryId, dto.getId())
                    .set(DishEntity::getCategoryId, targetCategoryId);
            dishService.update(moveWrapper);
        }
        baseMapper.deleteById(dto.getId());
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void sort(CategorySortDTO dto) {
        kitchenUserService.checkPermission(dto.getKitchenId());
        List<Long> ids = dto.getIds();
        for (int i = 0; i < ids.size(); i++) {
            LambdaUpdateWrapper<CategoryEntity> wrapper = new LambdaUpdateWrapper<CategoryEntity>()
                    .eq(CategoryEntity::getId, ids.get(i))
                    .eq(CategoryEntity::getKitchenId, dto.getKitchenId())
                    .set(CategoryEntity::getSort, i);
            update(wrapper);
        }
    }
}