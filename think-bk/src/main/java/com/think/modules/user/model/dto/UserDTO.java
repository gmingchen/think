package com.think.modules.user.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 好市民
 */
@Data
public class UserDTO {
    /**
     * ID
     */
    private Long id;
    /**
     * 微信openid
     */
    private String openid;
    /**
     * 微信开放平台统一标识
     */
    private String unionid;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 性别：0-女 1-男 2-未知
     */
    private Integer sex;
    /**
     * 邀请人用户ID
     */
    private Long inviterId;
    /**
     * 邀请用户数
     */
    private Integer inviteCount;
    /**
     * 创建时间（首次登录建行日，「我的」页「加入 · 第 N 天」展示用；由 BasePO 经 MapStruct 同名自动映射透出）
     */
    private LocalDateTime createdAt;
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
}
