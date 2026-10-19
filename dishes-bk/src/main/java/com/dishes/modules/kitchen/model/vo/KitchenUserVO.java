package com.dishes.modules.kitchen.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 厨房成员VO
 * @author 好市民
 */
@Data
public class KitchenUserVO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 角色：1-厨师长 2-管厨 3-食客
     */
    private Integer role;
    /**
     * 加入时间
     */
    private LocalDateTime createdAt;
}
