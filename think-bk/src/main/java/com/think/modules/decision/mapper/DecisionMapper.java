package com.think.modules.decision.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.decision.entity.DecisionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策单
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface DecisionMapper extends BaseMapperX<DecisionEntity> {

}
