package com.think.modules.log.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.log.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface OperationLogMapper extends BaseMapperX<OperationLogEntity> {

}
