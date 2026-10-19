package com.dishes.modules.member.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 查单兜底DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberConfirmDTO {
    /**
     * 下单时下发的业务单号
     */
    @NotBlank(message = "订单号不能为空")
    private String outTradeNo;
}
