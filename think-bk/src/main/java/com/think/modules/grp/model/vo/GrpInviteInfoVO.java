package com.think.modules.grp.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 邀请落地页信息（凭邀请码查群/邀请人概览 + 成员数/在评数，并给出可加入/失效态）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpInviteInfoVO {
    /**
     * 邀请是否仍可加入：false 表示码失效/过期或群已解散（前端进「失效态」）
     */
    private Boolean joinable;
    /**
     * 群ID（可加入时返回，供入群后跳群详情）
     */
    private Long grpId;
    /**
     * 群名称
     */
    private String grpName;
    /**
     * 群头像（预设标识，可空）
     */
    private String grpAvatar;
    /**
     * 在席成员数
     */
    private Integer memberCount;
    /**
     * 群内正在评议的决策数
     */
    private Integer reviewingCount;
    /**
     * 邀请人用户ID
     */
    private Long inviterId;
    /**
     * 邀请人昵称
     */
    private String inviterNickname;
    /**
     * 邀请人头像（无则前端回退昵称首字）
     */
    private String inviterAvatar;
}
