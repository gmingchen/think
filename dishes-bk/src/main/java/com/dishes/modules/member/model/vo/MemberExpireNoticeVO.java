package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 会员到期提醒的发送行（{@code MemberMapper#selectExpiringForNotice} 的结果投影，非实体）
 * <p>
 * 定时任务无登录态，openid 只能从 user 表按 user_id 关联取；tier 与 expireAt 来自 member。
 * 只装发送一条订阅消息所需的最小字段，不带任何资金/订单信息。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberExpireNoticeVO {
    /**
     * 会员所属用户ID（member.user_id）：订阅消息按 (userId, templateId) 扣本地配额，发送需要它
     */
    private Long userId;
    /**
     * 接收人 openid（user.openid）；为空说明该用户无 openid，微信侧会拒发，任务侧直接跳过
     */
    private String openid;
    /**
     * 库里档位原值：1-轻享 2-畅厨。查询已用 {@code tier > 0} 过滤，这里只可能是 1 或 2
     */
    private Integer tier;
    /**
     * 到期时间，用于渲染「到期时间」字段
     */
    private LocalDateTime expireAt;
}
