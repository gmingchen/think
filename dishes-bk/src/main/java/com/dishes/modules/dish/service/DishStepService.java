package com.dishes.modules.dish.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.dish.entity.DishStepEntity;
import com.dishes.modules.dish.model.dto.DishStepSaveUpdateDTO;
import com.dishes.modules.dish.model.vo.DishStepVO;

import java.util.List;

/**
 * 菜谱步骤
 * @author 好市民
 */
public interface DishStepService extends IServiceX<DishStepEntity> {

    List<DishStepVO> list(Long dishId);

    void create(DishStepSaveUpdateDTO dto);

    void update(DishStepSaveUpdateDTO dto);

    void deleteByDishId(Long dishId);
}
