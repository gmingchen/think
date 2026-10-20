package com.think.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户登录流水（v14，DAU 精确统计源，只增不改）
 * <p>
 * 每次小程序登录 INSERT 一行，登录时间即 {@code created_at}（BasePO INSERT 自动填充）。
 * 无更新 / 删除入口，deleted 恒为 0，保留 BasePO 四件套只为与全库结构一致。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "user_login_log", autoResultMap = true)
public class UserLoginLogEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 登录用户ID
     */
    private Long userId;
    /**
     * 微信openid
     */
    private String openid;
}
