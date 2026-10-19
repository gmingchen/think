package com.dishes.modules.kitchen.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 厨房邀请信息VO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenInviteInfoVO {
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 厨房名称
     */
    private String kitchenName;
    /**
     * 厨师长昵称
     */
    private String ownerName;
    /**
     * 邀请者昵称
     */
    private String inviterName;
    /**
     * 成员数
     */
    private Integer memberCount;
    /**
     * 菜谱数
     */
    private Integer dishCount;
    /**
     * 被邀请角色
     */
    private Integer role;
}
