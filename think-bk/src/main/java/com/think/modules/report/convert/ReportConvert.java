package com.think.modules.report.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.report.entity.ReportEntity;
import com.think.modules.report.model.dto.ReportDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 决策报告
 *
 * @author 好市民
 */
@Mapper
public interface ReportConvert extends BaseConvert {
    ReportConvert INSTANCE = Mappers.getMapper(ReportConvert.class);

    ReportDTO convert(ReportEntity bean);
}
