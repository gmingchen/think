package com.think.modules.recap.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.recap.entity.RecapEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策复盘
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface RecapMapper extends BaseMapperX<RecapEntity> {

}
