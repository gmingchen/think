package com.think.modules.review.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.review.entity.ReviewEntity;
import com.think.modules.review.model.dto.ReviewDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 评议
 *
 * @author 好市民
 */
@Mapper
public interface ReviewConvert extends BaseConvert {
    ReviewConvert INSTANCE = Mappers.getMapper(ReviewConvert.class);

    ReviewDTO convert(ReviewEntity bean);
}
