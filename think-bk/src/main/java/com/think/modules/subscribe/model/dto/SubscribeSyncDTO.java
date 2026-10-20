package com.think.modules.subscribe.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 订阅授权同步DTO：前端 requestSubscribeMessage 拿到 accept 后，把「这次授权」同步给后端累加配额
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class SubscribeSyncDTO {
    /**
     * 微信模板ID（前端授权用的那个，与后端 subscribe_template 里对应类型的 template_id 同值）
     */
    @NotBlank(message = "缺少模板ID")
    private String templateId;
    /**
     * 本次授权增加的可发次数，通常为 1（一次性订阅授权一次=可发一次）
     */
    @Min(value = 1, message = "同步次数至少为 1")
    private Integer count = 1;
}
