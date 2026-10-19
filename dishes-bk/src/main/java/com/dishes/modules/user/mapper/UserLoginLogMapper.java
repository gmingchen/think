package com.dishes.modules.user.mapper;

import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.user.entity.UserLoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户登录流水（v14）
 * @author 好市民
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapperX<UserLoginLogEntity> {

}
