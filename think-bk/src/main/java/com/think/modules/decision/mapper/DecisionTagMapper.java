package com.think.modules.decision.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.decision.entity.DecisionTagEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策单标签关联
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface DecisionTagMapper extends BaseMapperX<DecisionTagEntity> {

}
