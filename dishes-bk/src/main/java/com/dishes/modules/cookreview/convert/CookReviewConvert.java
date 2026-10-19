package com.dishes.modules.cookreview.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.cookreview.entity.CookReviewEntity;
import com.dishes.modules.cookreview.model.dto.CookReviewDishDTO;
import com.dishes.modules.cookreview.model.vo.CookReviewVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 烹饪评价
 * @author 好市民
 */
@Mapper
public interface CookReviewConvert extends BaseConvert {
    CookReviewConvert INSTANCE = Mappers.getMapper(CookReviewConvert.class);

    CookReviewVO convert(CookReviewEntity entity);

    @Mapping(target = "cookOrderId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CookReviewEntity convert(CookReviewDishDTO dto);
}
