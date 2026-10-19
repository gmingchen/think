package com.dishes.modules.cookorder.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.cookorder.entity.CookOrderDishEntity;
import com.dishes.modules.cookorder.entity.CookOrderEntity;
import com.dishes.modules.cookorder.model.dto.CookOrderCreateDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderUpdateDTO;
import com.dishes.modules.cookorder.model.vo.CookOrderDishVO;
import com.dishes.modules.cookorder.model.vo.CookOrderInfoVO;
import com.dishes.modules.cookorder.model.vo.CookOrderVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 烹饪订单
 * @author 好市民
 */
@Mapper
public interface CookOrderConvert extends BaseConvert {
    CookOrderConvert INSTANCE = Mappers.getMapper(CookOrderConvert.class);

    CookOrderVO convert(CookOrderEntity entity);

    CookOrderInfoVO convertInfo(CookOrderEntity entity);

    CookOrderEntity convertEntity(CookOrderCreateDTO dto);

    CookOrderEntity convertEntity(CookOrderUpdateDTO dto);

    CookOrderDishVO convertDishVO(CookOrderDishEntity entity);
}
