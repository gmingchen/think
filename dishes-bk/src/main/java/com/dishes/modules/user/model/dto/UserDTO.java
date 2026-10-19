package com.dishes.modules.user.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 好市民
 */
@Data
public class UserDTO {
    /**
     * ID
     */
    private Long id;
    /**
     * 微信openid
     */
    private String openid;
    /**
     * 微信开放平台统一标识
     */
    private String unionid;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 性别：0-女 1-男 2-未知
     */
    private Integer sex;
    /**
     * 邀请人用户ID
     */
    private Long inviterId;
    /**
     * 邀请用户数
     */
    private Integer inviteCount;
    /**
     * 个人邀请小程序码图片URL（海报归因，一人一码永久）
     */
    private String inviteQrcode;
    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginAt;
    /**
     * 创建时间（首次登录建行日，「我的」页「加入 · 第 N 天」展示用；由 BasePO 经 MapStruct 同名自动映射透出）
     */
    private LocalDateTime createdAt;
    /**
     * 会员档位：0-非会员 1-轻享 2-畅厨（v12）
     * <p>
     * ⚠️ 由 {@code member.tier} 按 expire_at &gt; now 归零后透出，绝不能直接拷库里原值：
     * 库里那一列的语义是「最近一次发放的档位」，过期行仍留着原值，
     * 不归零就会让过期会员的 memberTier 恒为 2 → 前端全站判据 {@code !user.memberTier} 失效 → 永久免广告（docs/v12 §8.1）。
     * 归零口径必须与 {@code MemberInfoVO.tier} 同源，只改一处就是漏改。
     * </p>
     */
    private Integer memberTier;
    /**
     * 会员到期时间原值（过期也照样透出，前端「已于 X 到期」这句话要用）
     */
    private LocalDateTime memberExpireAt;
    /**
     * 上次选择的厨房ID
     */
    private Long lastKitchenId;
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 读时判定：取快照里已归零的会员档位，再做一次到期比较（docs/v12 §6.3）
     * <p>
     * 写它而不让各校验点自己拼判断，是因为「memberTier &gt; 0 且 expireAt 晚于现在」这条规则
     * 有好几个出口（免广告、无限容量、厨房卡片）：一处写、多处读，才不会出现「改了会员判定但厨房页还在用老口径」。
     * 带参数故不会被 Jackson 当属性序列化，不会进 Redis 快照也不会上进接口返回。
     * </p>
     *
     * @param now 判定时刻
     * @return 在期返回档位，否则 0
     */
    public int memberTierAt(LocalDateTime now) {
        if (memberTier == null || memberTier <= 0) {
            return 0;
        }
        return memberExpireAt != null && now != null && memberExpireAt.isAfter(now) ? memberTier : 0;
    }
}
