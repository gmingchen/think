package com.dishes.modules.kitchen.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.kitchen.entity.KitchenEntity;
import com.dishes.modules.kitchen.entity.KitchenUserEntity;
import com.dishes.modules.kitchen.model.dto.KitchenUserCreateDTO;
import com.dishes.modules.kitchen.model.vo.KitchenVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 厨房
 * @author 好市民
 */
@Mapper
public interface KitchenConvert extends BaseConvert {
    KitchenConvert INSTANCE = Mappers.getMapper(KitchenConvert.class);

    KitchenVO convert(KitchenEntity bean);

    KitchenUserEntity convert(KitchenUserCreateDTO dto);
}
