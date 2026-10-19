package com.dishes.modules.food.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.model.vo.FoodVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 食材
 * @author 好市民
 */
@Mapper
public interface FoodConvert extends BaseConvert {
    FoodConvert INSTANCE = Mappers.getMapper(FoodConvert.class);

    FoodVO convert(FoodEntity entity);
}
