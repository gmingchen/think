package com.think.modules.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "user", autoResultMap = true)
public class UserEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 状态：0-禁用 1-启用
     */
    private Integer status;
}
