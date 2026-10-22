package com.think.modules.grp.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 群列表项（我的群列表 / 群详情头部复用）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpDetailVO {
    /**
     * 群ID
     */
    private Long id;
    /**
     * 群名称
     */
    private String name;
    /**
     * 群头像
     */
    private String avatar;
    /**
     * 群主用户ID
     */
    private Long userId;
    /**
     * 状态：1-正常 2-已解散
     */
    private Integer status;
    /**
     * 成员数（实时统计）
     */
    private Integer memberCount;
    /**
     * 我在该群的角色：1-群主 2-成员（非成员为 null）
     */
    private Integer myRole;
    /**
     * 群内决策单总数（群内可见档、全状态）：仅群详情接口返回，列表不查避免 N+1
     */
    private Integer decisionCount;
}
