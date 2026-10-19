package com.dishes.modules.category.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.category.entity.CategoryEntity;
import com.dishes.modules.category.model.vo.CategoryVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 分类
 * @author 好市民
 */
@Mapper
public interface CategoryConvert extends BaseConvert {
    CategoryConvert INSTANCE = Mappers.getMapper(CategoryConvert.class);

    CategoryVO convert(CategoryEntity entity);
}
