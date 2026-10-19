package com.dishes.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.modules.dish.convert.DishConvert;
import com.dishes.modules.dish.entity.DishStepEntity;
import com.dishes.modules.dish.mapper.DishStepMapper;
import com.dishes.modules.dish.model.dto.DishStepDTO;
import com.dishes.modules.dish.model.dto.DishStepSaveUpdateDTO;
import com.dishes.modules.dish.model.vo.DishStepVO;
import com.dishes.modules.dish.service.DishStepService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜品步骤
 * @author 好市民
 */
@Service("dishStepService")
public class DishStepServiceImpl extends ServiceImplX<DishStepMapper, DishStepEntity> implements DishStepService {

    @Override
    public List<DishStepVO> list(Long dishId) {
        LambdaQueryWrapper<DishStepEntity> wrapper = new LambdaQueryWrapper<DishStepEntity>()
                .eq(DishStepEntity::getDishId, dishId)
                .orderByAsc(DishStepEntity::getSort);
        List<DishStepEntity> steps = baseMapper.selectList(wrapper);
        return CollectionUtils.mapList(steps, DishConvert.INSTANCE::convertStepVO);
    }

    @Override
    public void create(DishStepSaveUpdateDTO dto) {
        List<DishStepDTO> list = dto.getList();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Long dishId = dto.getId();
        List<DishStepEntity> entities = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            DishStepDTO item = list.get(i);
            entities.add(DishConvert.INSTANCE.convertStepEntity(item)
                    .setDishId(dishId)
                    .setSort(item.getSort() != null ? item.getSort() : i));
        }
        saveBatch(entities);
    }

    @Override
    public void update(DishStepSaveUpdateDTO dto) {
        deleteByDishId(dto.getId());
        if (!CollectionUtils.isEmpty(dto.getList())) {
            create(dto);
        }
    }

    @Override
    public void deleteByDishId(Long dishId) {
        LambdaQueryWrapper<DishStepEntity> wrapper = new LambdaQueryWrapper<DishStepEntity>()
                .eq(DishStepEntity::getDishId, dishId);
        baseMapper.delete(wrapper);
    }
}
