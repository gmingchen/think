package com.dishes.modules.member.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 取支付签名DTO（docs/v12 §7.2 第二段）
 * <p>
 * 2026-10-11 把「创建订单」与「算签名」拆成两个接口后新增：本 DTO 就是签名那一步的唯一入参，
 * 与 {@link MemberConfirmDTO} 同构（都只认业务单号）。之所以用单号而不是套餐码——
 * 签名必须锁在<b>已落库的那一条订单快照</b>上，绝不允许按 planCode 现查价目表：
 * 两步之间运营正好改了价，就会出现「旧价建单、新价签名」→ 微信侧 -15013（§7.2 拆分要点 3）。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberPaySignDTO {
    /**
     * 创建订单时下发的业务单号，须是本人且仍处待支付状态的单
     */
    @NotBlank(message = "订单号不能为空")
    private String outTradeNo;
}
