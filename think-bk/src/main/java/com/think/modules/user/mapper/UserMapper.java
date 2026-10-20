package com.think.modules.user.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户
 * @author 拖孩
 * @email 1240235512@qq.com
 * @date 2023-07-22 17:36:32
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface UserMapper extends BaseMapperX<UserEntity> {

}
