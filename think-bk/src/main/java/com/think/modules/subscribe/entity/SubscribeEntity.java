package com.think.modules.subscribe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 订阅消息配额记录（一人一模板一行，记「还能给这个用户发几条」）
 * <p>
 * 微信一次性订阅「授权一次=可发一次」，但微信不提供「查某用户剩余配额」的接口，所以本地记一笔账：
 * 前端 {@code requestSubscribeMessage} 拿到 accept 后调 {@code /subscribe/sync} 累加，发送前 {@code consume} 原子扣减。
 * 本地账只是「要不要尝试发送」的闸门，真扣不到（用户早已授权但没记上）时最坏是漏发，不会误发。
 * </p>
 * <p>
 * ⚠️ 唯一索引 {@code uk_user_template(user_id, template_id)}：sync 走 upsert 靠它幂等。
 * 本表无删除入口，软删行仍占唯一键，勿随意 removeById。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "subscribe", autoResultMap = true)
public class SubscribeEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订阅用户ID
     */
    private Long userId;
    /**
     * 微信模板ID（与 user_id 组成唯一键）
     */
    private String templateId;
    /**
     * 剩余可发次数
     */
    private Integer remainingCount;
}
