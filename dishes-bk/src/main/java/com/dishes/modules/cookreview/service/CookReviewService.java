package com.dishes.modules.cookreview.service;

import com.dishes.common.pojo.PageResult;
import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.cookreview.entity.CookReviewEntity;
import com.dishes.modules.cookreview.model.dto.CookReviewDishDTO;
import com.dishes.modules.cookreview.model.dto.CookReviewPageDTO;
import com.dishes.modules.cookreview.model.vo.CookReviewVO;

import java.util.List;

/**
 * 烹饪评价
 * @author 好市民
 */
public interface CookReviewService extends IServiceX<CookReviewEntity> {

    PageResult<CookReviewVO> page(CookReviewPageDTO dto);

    void saveBatch(Long cookOrderId, Long userId, List<CookReviewDishDTO> dishes);
}
