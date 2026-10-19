package com.dishes.modules.seasoning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.common.utils.StringUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.seasoning.convert.SeasoningConvert;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.mapper.SeasoningMapper;
import com.dishes.modules.seasoning.model.dto.SeasoningCreateDTO;
import com.dishes.modules.seasoning.model.dto.SeasoningDeleteDTO;
import com.dishes.modules.seasoning.model.dto.SeasoningUpdateDTO;
import com.dishes.modules.seasoning.model.vo.SeasoningVO;
import com.dishes.modules.seasoning.service.SeasoningService;
import com.dishes.modules.kitchen.service.KitchenUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调料
 * @author 好市民
 */
@Service("seasoningService")
public class SeasoningServiceImpl extends ServiceImplX<SeasoningMapper, SeasoningEntity> implements SeasoningService {

    @Autowired
    private KitchenUserService kitchenUserService;

    @Override
    public List<SeasoningVO> listVO(Long kitchenId) {
        LambdaQueryWrapper<SeasoningEntity> wrapper = new LambdaQueryWrapper<SeasoningEntity>()
                .eq(SeasoningEntity::getKitchenId, kitchenId)
                .orderByDesc(SeasoningEntity::getUsageCount)
                .orderByDesc(SeasoningEntity::getUpdatedAt);
        List<SeasoningEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, SeasoningConvert.INSTANCE::convert);
    }

    @Override
    public Long create(SeasoningCreateDTO dto) {
        Long userId = SecurityUtils.getLoginUserId();
        kitchenUserService.checkPermission(dto.getKitchenId());
        // 检查同厨房下调料名是否重复
        LambdaQueryWrapper<SeasoningEntity> wrapper = new LambdaQueryWrapper<SeasoningEntity>()
                .eq(SeasoningEntity::getKitchenId, dto.getKitchenId())
                .eq(SeasoningEntity::getName, dto.getName());
        if (count(wrapper) > 0) {
            throw new RunException("调料名称已存在");
        }
        SeasoningEntity entity = new SeasoningEntity()
                .setKitchenId(dto.getKitchenId())
                .setUserId(userId)
                .setName(dto.getName())
                .setUsageCount(0);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(SeasoningUpdateDTO dto) {
        SeasoningEntity entity = checkPermission(dto.getId());
        // 检查同厨房下调料名是否重复（排除自身）
        LambdaQueryWrapper<SeasoningEntity> wrapper = new LambdaQueryWrapper<SeasoningEntity>()
                .eq(SeasoningEntity::getKitchenId, entity.getKitchenId())
                .eq(SeasoningEntity::getName, dto.getName())
                .ne(SeasoningEntity::getId, dto.getId());
        if (count(wrapper) > 0) {
            throw new RunException("调料名称已存在");
        }
        SeasoningEntity update = new SeasoningEntity()
                .setId(dto.getId())
                .setName(dto.getName());
        baseMapper.updateById(update);
    }

    @Override
    public void delete(SeasoningDeleteDTO dto) {
        SeasoningEntity entity = checkPermission(dto.getId());
        if (entity.getUsageCount() > 0) {
            throw new RunException("该调料正在被 " + entity.getUsageCount() + " 道菜谱使用，无法删除");
        }
        baseMapper.deleteById(dto.getId());
    }

    @Override
    public Map<String, Long> matchOrCreateByNames(Long kitchenId, List<String> names, Long userId) {
        Map<String, Long> matched = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(names)) {
            return matched;
        }
        // 查询厨房全部调料（含软删幽灵行）：唯一索引 uk_kitchen_name 覆盖软删行，
        // 若只匹配活跃行，遇到"删过又再次录入"的名字会新建并撞键，故一并捞出并支持复活。
        List<SeasoningEntity> all = baseMapper.selectAllIncludeDeleted(kitchenId);
        Map<String, SeasoningEntity> entityMap = new LinkedHashMap<>();
        for (SeasoningEntity entity : all) {
            String key = StringUtils.normalizeName(entity.getName());
            if (StringUtils.isBlank(key)) {
                continue;
            }
            SeasoningEntity exist = entityMap.get(key);
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
            SeasoningEntity exist = entityMap.get(key);
            Long seasoningId;
            if (exist == null) {
                // 库中确无该名字（含幽灵行），新建
                SeasoningEntity entity = new SeasoningEntity()
                        .setKitchenId(kitchenId)
                        .setUserId(userId)
                        .setName(name)
                        .setUsageCount(1);
                save(entity);
                seasoningId = entity.getId();
                entityMap.put(key, entity);
            } else if (Boolean.TRUE.equals(exist.getDeleted())) {
                // 幽灵行复活：撤销软删并使用次数 +1
                baseMapper.reviveById(exist.getId());
                exist.setDeleted(false);
                seasoningId = exist.getId();
            } else {
                // 活跃行，使用次数 +1
                LambdaUpdateWrapper<SeasoningEntity> updateWrapper = new LambdaUpdateWrapper<SeasoningEntity>()
                        .eq(SeasoningEntity::getId, exist.getId())
                        .setSql("usage_count = usage_count + 1");
                update(updateWrapper);
                seasoningId = exist.getId();
            }
            matched.put(key, seasoningId);
        }
        return matched;
    }

    private SeasoningEntity checkPermission(Long id) {
        SeasoningEntity entity = validateExists(id);
        kitchenUserService.checkPermission(entity.getKitchenId());
        return entity;
    }
}