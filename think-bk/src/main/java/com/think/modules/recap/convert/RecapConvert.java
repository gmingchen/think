package com.think.modules.recap.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.recap.entity.RecapEntity;
import com.think.modules.recap.model.dto.RecapDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 决策复盘
 *
 * @author 好市民
 */
@Mapper
public interface RecapConvert extends BaseConvert {
    RecapConvert INSTANCE = Mappers.getMapper(RecapConvert.class);

    RecapDTO convert(RecapEntity bean);
}
