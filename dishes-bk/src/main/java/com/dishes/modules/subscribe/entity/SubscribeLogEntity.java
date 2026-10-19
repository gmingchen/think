package com.dishes.modules.subscribe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 订阅消息发送流水（每次真正调用微信发送都记一行，成功失败都记）
 * <p>
 * 它是排查「为什么没收到」的唯一线索来源：微信侧对「用户没配额」只回一个 errcode，
 * 本地若不记就只能靠日志翻。这里把 touser、模板、类型、结果、失败原因固化下来。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "subscribe_log", autoResultMap = true)
public class SubscribeLogEntity extends BasePO {
    /**
     * 发送成功
     */
    public static final int STATUS_SUCCESS = 1;
    /**
     * 发送失败
     */
    public static final int STATUS_FAIL = 2;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 接收人用户ID
     */
    private Long userId;
    /**
     * 微信模板ID
     */
    private String templateId;
    /**
     * 通知类型，取值见 {@link com.dishes.common.enums.SubscribeTypeEnum}
     */
    private String type;
    /**
     * 发送状态：1-成功 2-失败
     */
    private Integer status;
    /**
     * 失败原因（微信 errmsg 或本地异常摘要）；成功可空
     */
    private String message;
}
