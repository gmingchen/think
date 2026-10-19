package com.dishes.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.exception.RunException;
import com.dishes.modules.dish.convert.DishConvert;
import com.dishes.modules.dish.entity.DishSeasoningEntity;
import com.dishes.modules.dish.mapper.DishSeasoningMapper;
import com.dishes.modules.dish.model.dto.DishSeasoningDTO;
import com.dishes.modules.dish.model.dto.DishSeasoningSaveUpdateDTO;
import com.dishes.modules.dish.model.vo.DishSeasoningVO;
import com.dishes.modules.dish.service.DishSeasoningService;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.service.SeasoningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品调料关联
 * @author 好市民
 */
@Service("dishSeasoningService")
public class DishSeasoningServiceImpl extends ServiceImplX<DishSeasoningMapper, DishSeasoningEntity> implements DishSeasoningService {

    @Autowired
    private SeasoningService seasoningService;

    @Override
    public List<DishSeasoningVO> list(Long dishId) {
        LambdaQueryWrapper<DishSeasoningEntity> wrapper = new LambdaQueryWrapper<DishSeasoningEntity>()
                .eq(DishSeasoningEntity::getDishId, dishId)
                .orderByAsc(DishSeasoningEntity::getSort);
        List<DishSeasoningEntity> dishSeasonings = baseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dishSeasonings)) {
            return java.util.Collections.emptyList();
        }
        List<Long> seasoningIds = CollectionUtils.mapList(dishSeasonings, DishSeasoningEntity::getSeasoningId);
        Map<Long, SeasoningEntity> seasoningMap = seasoningService.listByIds(seasoningIds).stream()
                .collect(Collectors.toMap(SeasoningEntity::getId, Function.identity()));
        return CollectionUtils.mapList(dishSeasonings, ds -> {
            DishSeasoningVO vo = DishConvert.INSTANCE.convertSeasoningVO(ds);
            SeasoningEntity seasoning = seasoningMap.get(ds.getSeasoningId());
            vo.setName(seasoning != null ? seasoning.getName() : null);
            return vo;
        });
    }

    @Override
    public Map<Long, List<DishSeasoningVO>> mapByDishIds(List<Long> dishIds) {
        if (CollectionUtils.isEmpty(dishIds)) {
            return java.util.Collections.emptyMap();
        }
        // 一次 in 查全部关联行 + 一次补调料名称，SQL 条数与菜数无关；组内保持 sort 升序
        LambdaQueryWrapper<DishSeasoningEntity> wrapper = new LambdaQueryWrapper<DishSeasoningEntity>()
                .in(DishSeasoningEntity::getDishId, dishIds)
                .orderByAsc(DishSeasoningEntity::getDishId)
                .orderByAsc(DishSeasoningEntity::getSort);
        List<DishSeasoningEntity> dishSeasonings = baseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dishSeasonings)) {
            return java.util.Collections.emptyMap();
        }
        List<Long> seasoningIds = CollectionUtils.mapList(dishSeasonings, DishSeasoningEntity::getSeasoningId);
        Map<Long, SeasoningEntity> seasoningMap = seasoningService.listByIds(seasoningIds).stream()
                .collect(Collectors.toMap(SeasoningEntity::getId, Function.identity()));
        return dishSeasonings.stream().collect(Collectors.groupingBy(DishSeasoningEntity::getDishId,
                Collectors.mapping(ds -> {
                    DishSeasoningVO vo = DishConvert.INSTANCE.convertSeasoningVO(ds);
                    SeasoningEntity seasoning = seasoningMap.get(ds.getSeasoningId());
                    vo.setName(seasoning != null ? seasoning.getName() : null);
                    return vo;
                }, Collectors.toList())));
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void create(DishSeasoningSaveUpdateDTO dto) {
        List<DishSeasoningDTO> list = dto.getList();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Long dishId = dto.getId();
        List<DishSeasoningEntity> entities = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            DishSeasoningDTO item = list.get(i);
            entities.add(DishConvert.INSTANCE.convertSeasoningEntity(item)
                    .setDishId(dishId)
                    .setIsOptional(item.getIsOptional() != null ? item.getIsOptional() : 0)
                    .setSort(item.getSort() != null ? item.getSort() : i));
        }
        saveBatch(entities);
        // 更新调料使用次数
        List<Long> seasoningIds = CollectionUtils.mapList(list, DishSeasoningDTO::getSeasoningId);
        LambdaUpdateWrapper<SeasoningEntity> wrapper = new LambdaUpdateWrapper<SeasoningEntity>()
                .in(SeasoningEntity::getId, seasoningIds)
                .setSql("usage_count = usage_count + 1");
        seasoningService.update(wrapper);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void update(DishSeasoningSaveUpdateDTO dto) {
        deleteByDishId(dto.getId());
        if (!CollectionUtils.isEmpty(dto.getList())) {
            create(dto);
        }
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void deleteByDishId(Long dishId) {
        // 查询旧关联
        LambdaQueryWrapper<DishSeasoningEntity> queryWrapper = new LambdaQueryWrapper<DishSeasoningEntity>()
                .eq(DishSeasoningEntity::getDishId, dishId);
        List<DishSeasoningEntity> oldSeasonings = baseMapper.selectList(queryWrapper);
        // 回补使用次数
        if (!CollectionUtils.isEmpty(oldSeasonings)) {
            List<Long> seasoningIds = CollectionUtils.mapList(oldSeasonings, DishSeasoningEntity::getSeasoningId);
            LambdaUpdateWrapper<SeasoningEntity> wrapper = new LambdaUpdateWrapper<SeasoningEntity>()
                    .in(SeasoningEntity::getId, seasoningIds)
                    .setSql("usage_count = GREATEST(usage_count - 1, 0)");
            seasoningService.update(wrapper);
        }
        // 删除关联
        LambdaQueryWrapper<DishSeasoningEntity> wrapper = new LambdaQueryWrapper<DishSeasoningEntity>()
                .eq(DishSeasoningEntity::getDishId, dishId);
        baseMapper.delete(wrapper);
    }
}
