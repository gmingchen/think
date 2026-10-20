package com.think.modules.auth.convert;

import com.think.common.convert.BaseConvert;
import com.think.modules.auth.model.LoginUser;
import com.think.modules.user.model.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 鉴权
 * @author 拖孩
 * @email 1240235512@qq.com
 * @date 2023-07-22 17:36:32
 */
@Mapper
public interface AuthConvert extends BaseConvert {
	AuthConvert INSTANCE = Mappers.getMapper(AuthConvert.class);

	LoginUser convert(UserDTO bean);
}
