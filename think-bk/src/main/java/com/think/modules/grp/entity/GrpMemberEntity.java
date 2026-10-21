package com.think.modules.grp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 群成员（群内可见性判定依据）
 * <p>
 * 唯一索引 {@code uk_grp_user(grp_id, user_id)}：同一用户在同一群内仅一行，退群走软删。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "grp_member", autoResultMap = true)
public class GrpMemberEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
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
     * 角色：1-群主 2-成员（GrpMemberRoleEnum）
     */
    private Integer role;
}
