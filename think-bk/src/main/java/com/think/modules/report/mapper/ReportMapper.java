package com.think.modules.report.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.report.entity.ReportEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策报告
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface ReportMapper extends BaseMapperX<ReportEntity> {

}
