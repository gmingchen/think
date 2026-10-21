package com.think.modules.grp.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.grp.entity.GrpMemberEntity;
import com.think.modules.grp.model.dto.GrpMemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 群成员
 *
 * @author 好市民
 */
@Mapper
public interface GrpMemberConvert extends BaseConvert {
    GrpMemberConvert INSTANCE = Mappers.getMapper(GrpMemberConvert.class);

    GrpMemberDTO convert(GrpMemberEntity bean);
}
