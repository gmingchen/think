package com.think.modules.user.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 生成带邀请人参数的小程序码
 * @author 好市民
 */
@Data
public class UserQrcodeDTO {
    /**
     * 扫码落地页路径（getUnlimited 的 page，不带前导斜杠）
     */
    @NotBlank(message = "页面路径不能为空")
    private String page;
}
