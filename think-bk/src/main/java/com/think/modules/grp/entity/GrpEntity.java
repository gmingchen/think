package com.think.modules.grp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 群组（§3.1 群只是分组概念，无强身份）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "grp", autoResultMap = true)
public class GrpEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 群名称
     */
    private String name;
    /**
     * 群头像（预设标识，群主可修改）
     */
    private String avatar;
    /**
     * 群主用户ID
     */
    private Long userId;
    /**
     * 状态：1-正常 2-已解散（GrpStatusEnum）
     */
    private Integer status;
}
