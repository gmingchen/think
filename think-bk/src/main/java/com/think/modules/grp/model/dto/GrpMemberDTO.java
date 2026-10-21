package com.think.modules.grp.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员信息
 *
 * @author 好市民
 */
@Data
public class GrpMemberDTO {
    /**
     * ID
     */
    private Long id;
    /**
     * 所属群ID
     */
    private Long grpId;
    /**
     * 成员用户ID
     */
    private Long userId;
    /**
     * 角色：1-群主 2-成员
     */
    private Integer role;
    /**
     * 加入时间
     */
    private LocalDateTime createdAt;
}
