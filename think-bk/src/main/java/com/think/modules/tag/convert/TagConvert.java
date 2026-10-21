package com.think.modules.tag.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.tag.entity.TagEntity;
import com.think.modules.tag.model.dto.TagDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 标签
 *
 * @author 好市民
 */
@Mapper
public interface TagConvert extends BaseConvert {
    TagConvert INSTANCE = Mappers.getMapper(TagConvert.class);

    TagDTO convert(TagEntity bean);
}
