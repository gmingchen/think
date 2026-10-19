package com.dishes.modules.auth.model.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author 好市民
 */
@Data
public class LoginReqVO {
    /**
     * 小程序登录时获取的 code
     */
    @NotBlank(message = "code不能为空")
    private String code;
    /**
     * 邀请者用户ID
     */
    private Long inviter;
}
