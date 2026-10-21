package com.think.modules.abuse.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.abuse.entity.AbuseReportEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface AbuseReportMapper extends BaseMapperX<AbuseReportEntity> {

}
