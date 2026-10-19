package com.dishes.modules.dish.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.dish.entity.DishEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜谱
 * @author 好市民
 */
@Mapper
public interface DishMapper extends BaseMapperX<DishEntity> {

}