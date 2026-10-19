package com.dishes.modules.seasoning.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.model.vo.SeasoningVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 调料
 * @author 好市民
 */
@Mapper
public interface SeasoningConvert extends BaseConvert {
    SeasoningConvert INSTANCE = Mappers.getMapper(SeasoningConvert.class);

    SeasoningVO convert(SeasoningEntity entity);
}
