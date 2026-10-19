package com.dishes.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.exception.RunException;
import com.dishes.modules.dish.convert.DishConvert;
import com.dishes.modules.dish.entity.DishFoodEntity;
import com.dishes.modules.dish.mapper.DishFoodMapper;
import com.dishes.modules.dish.model.dto.DishFoodDTO;
import com.dishes.modules.dish.model.dto.DishFoodSaveUpdateDTO;
import com.dishes.modules.dish.model.vo.DishFoodVO;
import com.dishes.modules.dish.service.DishFoodService;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.service.FoodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品食材关联
 * @author 好市民
 */
@Service("dishFoodService")
public class DishFoodServiceImpl extends ServiceImplX<DishFoodMapper, DishFoodEntity> implements DishFoodService {

    @Autowired
    private FoodService foodService;

    @Override
    public List<DishFoodVO> list(Long dishId) {
        LambdaQueryWrapper<DishFoodEntity> wrapper = new LambdaQueryWrapper<DishFoodEntity>()
                .eq(DishFoodEntity::getDishId, dishId)
                .orderByAsc(DishFoodEntity::getSort);
        List<DishFoodEntity> dishFoods = baseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dishFoods)) {
            return java.util.Collections.emptyList();
        }
        List<Long> foodIds = CollectionUtils.mapList(dishFoods, DishFoodEntity::getFoodId);
        Map<Long, FoodEntity> foodMap = foodService.listByIds(foodIds).stream()
                .collect(Collectors.toMap(FoodEntity::getId, Function.identity()));
        return CollectionUtils.mapList(dishFoods, df -> {
            DishFoodVO vo = DishConvert.INSTANCE.convertFoodVO(df);
            FoodEntity food = foodMap.get(df.getFoodId());
            vo.setFoodName(food != null ? food.getName() : null);
            return vo;
        });
    }

    @Override
    public Map<Long, List<DishFoodVO>> mapByDishIds(List<Long> dishIds) {
        if (CollectionUtils.isEmpty(dishIds)) {
            return java.util.Collections.emptyMap();
        }
        // 一次 in 查全部关联行 + 一次补食材名称，SQL 条数与菜数无关；组内保持 sort 升序
        LambdaQueryWrapper<DishFoodEntity> wrapper = new LambdaQueryWrapper<DishFoodEntity>()
                .in(DishFoodEntity::getDishId, dishIds)
                .orderByAsc(DishFoodEntity::getDishId)
                .orderByAsc(DishFoodEntity::getSort);
        List<DishFoodEntity> dishFoods = baseMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dishFoods)) {
            return java.util.Collections.emptyMap();
        }
        List<Long> foodIds = CollectionUtils.mapList(dishFoods, DishFoodEntity::getFoodId);
        Map<Long, FoodEntity> foodMap = foodService.listByIds(foodIds).stream()
                .collect(Collectors.toMap(FoodEntity::getId, Function.identity()));
        return dishFoods.stream().collect(Collectors.groupingBy(DishFoodEntity::getDishId,
                Collectors.mapping(df -> {
                    DishFoodVO vo = DishConvert.INSTANCE.convertFoodVO(df);
                    FoodEntity food = foodMap.get(df.getFoodId());
                    vo.setFoodName(food != null ? food.getName() : null);
                    return vo;
                }, Collectors.toList())));
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void create(DishFoodSaveUpdateDTO dto) {
        List<DishFoodDTO> list = dto.getList();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Long dishId = dto.getId();
        List<DishFoodEntity> entities = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            DishFoodDTO item = list.get(i);
            entities.add(DishConvert.INSTANCE.convertFoodEntity(item)
                    .setDishId(dishId)
                    .setIsOptional(item.getIsOptional() != null ? item.getIsOptional() : 0)
                    .setSort(item.getSort() != null ? item.getSort() : i));
        }
        saveBatch(entities);
        // 更新食材使用次数
        List<Long> foodIds = CollectionUtils.mapList(list, DishFoodDTO::getFoodId);
        LambdaUpdateWrapper<FoodEntity> wrapper = new LambdaUpdateWrapper<FoodEntity>()
                .in(FoodEntity::getId, foodIds)
                .setSql("usage_count = usage_count + 1");
        foodService.update(wrapper);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void update(DishFoodSaveUpdateDTO dto) {
        deleteByDishId(dto.getId());
        if (!CollectionUtils.isEmpty(dto.getList())) {
            create(dto);
        }
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void deleteByDishId(Long dishId) {
        // 查询旧关联
        LambdaQueryWrapper<DishFoodEntity> queryWrapper = new LambdaQueryWrapper<DishFoodEntity>()
                .eq(DishFoodEntity::getDishId, dishId);
        List<DishFoodEntity> oldFoods = baseMapper.selectList(queryWrapper);
        // 回补使用次数
        if (!CollectionUtils.isEmpty(oldFoods)) {
            List<Long> foodIds = CollectionUtils.mapList(oldFoods, DishFoodEntity::getFoodId);
            LambdaUpdateWrapper<FoodEntity> wrapper = new LambdaUpdateWrapper<FoodEntity>()
                    .in(FoodEntity::getId, foodIds)
                    .setSql("usage_count = GREATEST(usage_count - 1, 0)");
            foodService.update(wrapper);
        }
        // 删除关联
        LambdaQueryWrapper<DishFoodEntity> wrapper = new LambdaQueryWrapper<DishFoodEntity>()
                .eq(DishFoodEntity::getDishId, dishId);
        baseMapper.delete(wrapper);
    }
}
