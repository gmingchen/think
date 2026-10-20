package com.think.modules.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.think.modules.user.model.dto.UserDTO;
import lombok.Data;

/**
 * 登录用户的信息
 * @author 好市民
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser extends UserDTO {

}
