package com.think.modules.user.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class UserCreateDTO {
    /**
     * 微信openid
     */
    private String openid;
    /**
     * 微信unionid
     */
    private String unionid;
    /**
     * 邀请人用户ID
     */
    private Long inviterId;
}
