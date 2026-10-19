package com.dishes.modules.dish.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.dish.entity.DishSeasoningEntity;
import com.dishes.modules.dish.model.dto.DishSeasoningSaveUpdateDTO;
import com.dishes.modules.dish.model.vo.DishSeasoningVO;

import java.util.List;
import java.util.Map;

/**
 * 菜谱调料关联
 * @author 好市民
 */
public interface DishSeasoningService extends IServiceX<DishSeasoningEntity> {

    List<DishSeasoningVO> list(Long dishId);

    /**
     * 批量查询多道菜的调料列表（采购清单），按 dishId 分组
     * @param dishIds 菜品ID列表
     * @return dishId -> 调料列表（组内按 sort 升序）
     */
    Map<Long, List<DishSeasoningVO>> mapByDishIds(List<Long> dishIds);

    void create(DishSeasoningSaveUpdateDTO dto);

    void update(DishSeasoningSaveUpdateDTO dto);

    void deleteByDishId(Long dishId);
}
