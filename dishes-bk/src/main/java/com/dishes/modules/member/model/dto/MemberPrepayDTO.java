package com.dishes.modules.member.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 下单DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberPrepayDTO {
    /**
     * 套餐编码，必须在 member_plan 里存在且已上架，否则拒单
     */
    @NotBlank(message = "请选择套餐")
    private String planCode;
}
