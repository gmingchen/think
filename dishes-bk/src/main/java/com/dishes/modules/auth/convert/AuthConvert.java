package com.dishes.modules.auth.convert;

import com.dishes.common.convert.BaseConvert;
import com.dishes.modules.auth.model.LoginUser;
import com.dishes.modules.user.model.dto.UserDTO;
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
