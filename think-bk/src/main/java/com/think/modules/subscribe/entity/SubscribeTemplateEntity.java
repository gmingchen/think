package com.think.modules.subscribe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 订阅消息模板配置（type → 微信模板ID 的唯一映射）
 * <p>
 * 微信那串模板 ID 只存在这一张表里，业务侧一律按 {@code type} 找它，不在代码/yml 里再写一份（v9 注意事项 3）。
 * 换模板只改本表这一行，不动代码、不发版。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "subscribe_template", autoResultMap = true)
public class SubscribeTemplateEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 通知类型，取值见 {@link com.think.common.enums.SubscribeTypeEnum}（唯一索引 uk_type）
     */
    private String type;
    /**
     * 微信订阅消息模板 ID；空串视为该类型未配置，发送时跳过
     */
    private String templateId;
    /**
     * 备注（如「接单通知」，便于后台辨认）
     */
    private String remark;
}
