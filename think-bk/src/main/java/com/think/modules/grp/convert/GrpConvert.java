package com.think.modules.grp.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.grp.entity.GrpEntity;
import com.think.modules.grp.model.dto.GrpDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 群组
 *
 * @author 好市民
 */
@Mapper
public interface GrpConvert extends BaseConvert {
    GrpConvert INSTANCE = Mappers.getMapper(GrpConvert.class);

    GrpDTO convert(GrpEntity bean);
}
