package com.dishes.modules.dish.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.dish.entity.DishFoodEntity;
import com.dishes.modules.dish.model.dto.DishFoodSaveUpdateDTO;
import com.dishes.modules.dish.model.vo.DishFoodVO;

import java.util.List;
import java.util.Map;

/**
 * 菜谱食材关联
 * @author 好市民
 */
public interface DishFoodService extends IServiceX<DishFoodEntity> {

    List<DishFoodVO> list(Long dishId);

    /**
     * 批量查询多道菜的食材列表（采购清单），按 dishId 分组
     * @param dishIds 菜品ID列表
     * @return dishId -> 食材列表（组内按 sort 升序）
     */
    Map<Long, List<DishFoodVO>> mapByDishIds(List<Long> dishIds);

    void create(DishFoodSaveUpdateDTO dto);

    void update(DishFoodSaveUpdateDTO dto);

    void deleteByDishId(Long dishId);
}
