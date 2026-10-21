package com.think.modules.decision.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.decision.entity.DecisionCheckEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 决策单内容审核流水
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface DecisionCheckMapper extends BaseMapperX<DecisionCheckEntity> {

}
