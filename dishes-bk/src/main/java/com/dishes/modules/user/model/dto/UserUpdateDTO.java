package com.dishes.modules.user.model.dto;

import com.dishes.common.enums.SexEnum;
import com.dishes.core.validator.constraints.Enum;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 用户信息编辑DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class UserUpdateDTO {
    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 性别：0-女 1-男 2-未知
     */
    @Enum(value = SexEnum.class, message = "性别值不合法")
    private Integer sex;
}
