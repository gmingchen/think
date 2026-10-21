package com.think.modules.user.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户简要信息（列表/详情里展示发起人、评议人、群成员用，仅暴露公开字段）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class UserBriefVO {
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
}
