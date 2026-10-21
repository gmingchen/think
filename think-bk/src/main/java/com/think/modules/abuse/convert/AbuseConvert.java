package com.think.modules.abuse.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.abuse.entity.AbuseReportEntity;
import com.think.modules.abuse.model.dto.AbuseReportDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 举报
 *
 * @author 好市民
 */
@Mapper
public interface AbuseConvert extends BaseConvert {
    AbuseConvert INSTANCE = Mappers.getMapper(AbuseConvert.class);

    AbuseReportDTO convert(AbuseReportEntity bean);
}
