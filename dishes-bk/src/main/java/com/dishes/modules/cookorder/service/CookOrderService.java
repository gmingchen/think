package com.dishes.modules.cookorder.service;

import com.dishes.common.pojo.PageResult;
import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.cookorder.entity.CookOrderEntity;
import com.dishes.modules.cookorder.model.dto.CookOrderAcceptDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderCancelDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderCreateDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderDeleteDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderPageDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderServeDTO;
import com.dishes.modules.cookorder.model.dto.CookOrderUpdateDTO;
import com.dishes.modules.cookorder.model.vo.CookOrderInfoVO;
import com.dishes.modules.cookorder.model.vo.CookOrderVO;
import com.dishes.modules.cookreview.model.dto.CookReviewCreateDTO;

import java.util.List;

/**
 * 烹饪订单
 * @author 好市民
 */
public interface CookOrderService extends IServiceX<CookOrderEntity> {

    PageResult<CookOrderVO> page(CookOrderPageDTO dto);

    /**
     * 查询当前时段订单（根据当前时间自动判断早餐/午餐/晚餐/宵夜）
     * @param kitchenId 厨房ID
     * @return 订单列表（含菜品）
     */
    List<CookOrderInfoVO> today(Long kitchenId);

    /**
     * 查询最近一餐的订单（取今天最新时段的所有订单）
     * @param kitchenId 厨房ID
     * @return 订单列表（含菜品）
     */
    List<CookOrderInfoVO> latest(Long kitchenId);

    CookOrderInfoVO info(Long id);

    Long create(CookOrderCreateDTO dto);

    void update(CookOrderUpdateDTO dto);

    void delete(CookOrderDeleteDTO dto);

    void accept(CookOrderAcceptDTO dto);

    void serve(CookOrderServeDTO dto);

    void cancel(CookOrderCancelDTO dto);

    void review(CookReviewCreateDTO dto);
}
