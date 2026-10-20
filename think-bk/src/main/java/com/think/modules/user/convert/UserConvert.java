package com.think.modules.user.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.user.entity.UserEntity;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.model.dto.UserUpdateDTO;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 用户
 * @author 拖孩
 * @email 1240235512@qq.com
 * @date 2023-07-22 17:36:32
 */
@Mapper
public interface UserConvert extends BaseConvert {
	UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

	UserDTO convert(UserEntity bean);

	UserEntity convertEntity(UserUpdateDTO dto);
}
