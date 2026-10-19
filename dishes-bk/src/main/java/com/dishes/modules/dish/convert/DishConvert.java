package com.dishes.modules.dish.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.entity.DishFoodEntity;
import com.dishes.modules.dish.entity.DishSeasoningEntity;
import com.dishes.modules.dish.entity.DishStepEntity;
import com.dishes.modules.dish.model.dto.DishCreateDTO;
import com.dishes.modules.dish.model.dto.DishUpdateDTO;
import com.dishes.modules.dish.model.dto.DishFoodDTO;
import com.dishes.modules.dish.model.dto.DishSeasoningDTO;
import com.dishes.modules.dish.model.dto.DishStepDTO;
import com.dishes.modules.dish.model.vo.DishFoodVO;
import com.dishes.modules.dish.model.vo.DishInfoVO;
import com.dishes.modules.dish.model.vo.DishSeasoningVO;
import com.dishes.modules.dish.model.vo.DishStepVO;
import com.dishes.modules.dish.model.vo.DishVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 菜谱
 * @author 好市民
 */
@Mapper
public interface DishConvert extends BaseConvert {
    DishConvert INSTANCE = Mappers.getMapper(DishConvert.class);

    DishVO convert(DishEntity entity);

    DishInfoVO convertInfo(DishEntity entity);

    DishEntity convertEntity(DishCreateDTO dto);

    DishEntity convertEntity(DishUpdateDTO dto);

    DishFoodEntity convertFoodEntity(DishFoodDTO dto);

    DishSeasoningEntity convertSeasoningEntity(DishSeasoningDTO dto);

    DishStepEntity convertStepEntity(DishStepDTO dto);

    DishFoodVO convertFoodVO(DishFoodEntity entity);

    DishSeasoningVO convertSeasoningVO(DishSeasoningEntity entity);

    DishStepVO convertStepVO(DishStepEntity entity);
}
