package com.think.modules.decision.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.model.dto.DecisionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 决策单
 *
 * @author 好市民
 */
@Mapper
public interface DecisionConvert extends BaseConvert {
    DecisionConvert INSTANCE = Mappers.getMapper(DecisionConvert.class);

    DecisionDTO convert(DecisionEntity bean);
}
