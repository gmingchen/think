package com.dishes.modules.preset.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.preset.entity.PresetCategoryEntity;
import com.dishes.modules.preset.entity.PresetFoodEntity;
import com.dishes.modules.preset.entity.PresetSeasoningEntity;
import com.dishes.modules.preset.model.vo.PresetItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 预设
 * @author 好市民
 */
@Mapper
public interface PresetConvert extends BaseConvert {
    PresetConvert INSTANCE = Mappers.getMapper(PresetConvert.class);

    PresetItemVO convert(PresetCategoryEntity bean);

    PresetItemVO convert(PresetFoodEntity bean);

    PresetItemVO convert(PresetSeasoningEntity bean);
}
