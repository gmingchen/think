package com.think.modules.grp.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.grp.entity.GrpEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface GrpMapper extends BaseMapperX<GrpEntity> {

}
