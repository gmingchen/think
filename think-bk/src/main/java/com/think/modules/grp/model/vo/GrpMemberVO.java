package com.think.modules.grp.model.vo;

import com.think.modules.user.model.vo.UserBriefVO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 群成员展示项（成员头像/昵称/角色/加入时间）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpMemberVO {
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
    private LocalDateTime joinedAt;
    /**
     * 成员用户简要信息
     */
    private UserBriefVO user;
}
