package com.dishes.modules.dish.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.dish.entity.DishFoodEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜谱食材关联
 * @author 好市民
 */
@Mapper
public interface DishFoodMapper extends BaseMapperX<DishFoodEntity> {

}
