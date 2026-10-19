package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 查单兜底结果VO（docs/v12 §7.3）
 * <p>
 * ⚠️ §9.1 的文件清单里原本没有这个 VO，但 §7.3 明确要回 {@code delivered}，
 * 所以它是按接口契约补的第 6 个 VO（清单其余部分未变）。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberConfirmVO {
    /**
     * 权益是否已到账。false 不代表失败——前端据此继续轮询，最后交给发货通知兜底
     */
    private Boolean delivered;
    /**
     * 订单当前状态：0-待支付 1-已发放 2-已退款 3-已关闭（金额不符/校验失败）。
     * 供前端区分「发放确认中」与「这单已关闭」两种不同的等待文案
     */
    private Integer status;
}
