package com.dishes.modules.cookorder.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.cookorder.entity.CookOrderDishEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 烹饪订单菜品关联
 * @author 好市民
 */
@Mapper
public interface CookOrderDishMapper extends BaseMapperX<CookOrderDishEntity> {

}
