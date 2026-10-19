package com.dishes.modules.cookorder.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.cookorder.entity.CookOrderDishEntity;
import com.dishes.modules.cookorder.model.dto.CookOrderDishDTO;
import com.dishes.modules.cookorder.model.vo.CookOrderDishVO;

import java.util.List;

/**
 * 烹饪订单菜品关联
 * @author 好市民
 */
public interface CookOrderDishService extends IServiceX<CookOrderDishEntity> {

    List<CookOrderDishVO> list(Long cookOrderId);

    void create(Long cookOrderId, List<CookOrderDishDTO> dishes);

    void deleteByCookOrderId(Long cookOrderId);
}
