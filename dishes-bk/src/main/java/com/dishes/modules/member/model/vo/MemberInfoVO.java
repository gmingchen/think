package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员状态聚合VO：一次请求把「我是谁 + 能买什么 + 买过什么 + 卡片怎么画」全给前端
 * <p>
 * 前端（会员中心页与「我的」页会员卡）只渲染不判断，价格也不硬编码（docs/v12 §7.1）。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberInfoVO {
    /**
     * 会员功能是否已放开（配置项 {@code member.enabled}，本版发版态为 false）。
     * <p>
     * ⚠️ 这个开关只能由后端下发，前端不许从 {@code mineCard.tone} 或 {@code action} 文案反推：
     * 置灰期里一个<b>生效中</b>的人 tone 仍是 {@code gold}，只有 action 是「即将开放」，
     * 拿字符串比对去关按钮就又是一份「前端自己判断」的真相（§5.2、§7.1）。
     * </p>
     */
    private Boolean enabled;
    /**
     * 当前有效档位（0/1/2）。⚠️ 已按 expireAt 归零，与 effective 同源：
     * 库里那一列的语义是「最近一次发放的档位」，不过归零就会出现 tier=2 且 effective=false 的自相矛盾入参
     */
    private Integer tier;
    /**
     * 是否在生效期
     */
    private Boolean effective;
    /**
     * 到期时间原值（过期用户的「已于 X 到期」这句话要用，所以不能因为过期就置 null）
     */
    private LocalDateTime expireAt;
    /**
     * 剩余天数（向上取整），非会员为 0
     */
    private Integer remainDays;
    /**
     * 免广告（一档二档都免）
     */
    private Boolean adFree;
    /**
     * 分类与菜谱不限数（仅二档）
     */
    private Boolean unlimitedCapacity;
    /**
     * 是否享畅厨·月首单价：按「有没有 tier=2 的已发放订单」判，不是按有没有付过费（§7.2 要点 3）
     */
    private Boolean firstOrder;
    /**
     * 在售套餐，按 member_plan.sort 下发；enabled=0 的不含（前端自动少一个胶囊，零代码退档）
     */
    private List<MemberPlanVO> plans;
    /**
     * 「我的」页会员卡文案与色调
     */
    private MemberMineCardVO mineCard;
    /**
     * 本人最近 5 条已发放/已退款订单
     */
    private List<MemberRecordVO> records;
}
