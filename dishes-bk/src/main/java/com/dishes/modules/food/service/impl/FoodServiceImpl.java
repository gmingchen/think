package com.dishes.modules.food.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.common.utils.StringUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.food.convert.FoodConvert;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.mapper.FoodMapper;
import com.dishes.modules.food.model.dto.FoodCreateDTO;
import com.dishes.modules.food.model.dto.FoodDeleteDTO;
import com.dishes.modules.food.model.dto.FoodUpdateDTO;
import com.dishes.modules.food.model.vo.FoodVO;
import com.dishes.modules.food.service.FoodService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 食材
 * @author 好市民
 */
@Service("foodService")
public class FoodServiceImpl extends ServiceImplX<FoodMapper, FoodEntity> implements FoodService {

    @Autowired
    private KitchenUserService kitchenUserService;

    @Override
    public List<FoodVO> listVO(Long kitchenId) {
        LambdaQueryWrapper<FoodEntity> wrapper = new LambdaQueryWrapper<FoodEntity>()
                .eq(FoodEntity::getKitchenId, kitchenId)
                .orderByDesc(FoodEntity::getUsageCount)
                .orderByDesc(FoodEntity::getUpdatedAt);
        List<FoodEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, FoodConvert.INSTANCE::convert);
    }

    @Override
    public Long create(FoodCreateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        kitchenUserService.checkPermission(dto.getKitchenId());
        // 检查同厨房下食材名是否重复
        LambdaQueryWrapper<FoodEntity> wrapper = new LambdaQueryWrapper<FoodEntity>()
                .eq(FoodEntity::getKitchenId, dto.getKitchenId())
                .eq(FoodEntity::getName, dto.getName());
        if (count(wrapper) > 0) {
            throw new RunException("食材名称已存在");
        }
        FoodEntity entity = new FoodEntity()
                .setKitchenId(dto.getKitchenId())
                .setUserId(userId)
                .setName(dto.getName())
                .setUsageCount(0);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(FoodUpdateDTO dto) {
        FoodEntity entity = checkPermission(dto.getId());
        // 检查同厨房下食材名是否重复（排除自身）
        LambdaQueryWrapper<FoodEntity> wrapper = new LambdaQueryWrapper<FoodEntity>()
                .eq(FoodEntity::getKitchenId, entity.getKitchenId())
                .eq(FoodEntity::getName, dto.getName())
                .ne(FoodEntity::getId, dto.getId());
        if (count(wrapper) > 0) {
            throw new RunException("食材名称已存在");
        }
        FoodEntity update = new FoodEntity()
                .setId(dto.getId())
                .setName(dto.getName());
        baseMapper.updateById(update);
    }

    @Override
    public void delete(FoodDeleteDTO dto) {
        FoodEntity entity = checkPermission(dto.getId());
        if (entity.getUsageCount() > 0) {
            throw new RunException("该食材正在被 " + entity.getUsageCount() + " 道菜谱使用，无法删除");
        }
        baseMapper.deleteById(dto.getId());
    }

    @Override
    public Map<String, Long> matchOrCreateByNames(Long kitchenId, List<String> names, Long userId) {
        Map<String, Long> matched = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(names)) {
            return matched;
        }
        // 查询厨房全部食材（含软删幽灵行）：唯一索引 uk_kitchen_name 覆盖软删行，
        // 若只匹配活跃行，遇到"删过又再次录入"的名字会新建并撞键，故一并捞出并支持复活。
        List<FoodEntity> all = baseMapper.selectAllIncludeDeleted(kitchenId);
        Map<String, FoodEntity> entityMap = new LinkedHashMap<>();
        for (FoodEntity entity : all) {
            String key = StringUtils.normalizeName(entity.getName());
            if (StringUtils.isBlank(key)) {
                continue;
            }
            FoodEntity exist = entityMap.get(key);
            // 同名（归一化）优先保留活跃行，避免幽灵行遮蔽可用记录
            if (exist == null || (Boolean.TRUE.equals(exist.getDeleted()) && !Boolean.TRUE.equals(entity.getDeleted()))) {
                entityMap.put(key, entity);
            }
        }
        for (String rawName : names) {
            String name = StringUtils.trim(rawName);
            String key = StringUtils.normalizeName(name);
            if (StringUtils.isBlank(key)) {
                continue;
            }
            FoodEntity exist = entityMap.get(key);
            Long foodId;
            if (exist == null) {
                // 库中确无该名字（含幽灵行），新建
                FoodEntity entity = new FoodEntity()
                        .setKitchenId(kitchenId)
                        .setUserId(userId)
                        .setName(name)
                        .setUsageCount(1);
                save(entity);
                foodId = entity.getId();
                entityMap.put(key, entity);
            } else if (Boolean.TRUE.equals(exist.getDeleted())) {
                // 幽灵行复活：撤销软删并使用次数 +1
                baseMapper.reviveById(exist.getId());
                exist.setDeleted(false);
                foodId = exist.getId();
            } else {
                // 活跃行，使用次数 +1
                LambdaUpdateWrapper<FoodEntity> updateWrapper = new LambdaUpdateWrapper<FoodEntity>()
                        .eq(FoodEntity::getId, exist.getId())
                        .setSql("usage_count = usage_count + 1");
                update(updateWrapper);
                foodId = exist.getId();
            }
            matched.put(key, foodId);
        }
        return matched;
    }

    private FoodEntity checkPermission(Long id) {
        FoodEntity entity = validateExists(id);
        kitchenUserService.checkPermission(entity.getKitchenId());
        return entity;
    }
}