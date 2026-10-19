package com.dishes.modules.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 会员状态（一人一行，只 UPDATE，无业务删除入口）
 * <p>
 * ⚠️ {@code tier} 的语义是「最近一次发放的档位」，不是「当前有效档位」：
 * 有效性只靠 {@code expireAt > now} 在读时判定，且本版没有回收任务，
 * 所以过期行的 tier 会永远留着原值。任何出口（UserDTO / MemberInfoVO）都必须先归零再透出，
 * 否则过期会员会永久免广告（docs/v12 §8.1）。
 * </p>
 * <p>
 * ⚠️ 本表禁止调用 {@code removeById} / {@code remove(wrapper)}：
 * 一旦软删了某行，该用户再开通时 upsert 查不到旧行会走 INSERT，直接撞 {@code uk_user_id}。
 * 确有清理需求走物理 DELETE，或先把唯一索引改成 uk_user_id_deleted（§8.1）。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "member", autoResultMap = true)
public class MemberEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 用户ID（唯一索引 uk_user_id）
     */
    private Long userId;
    /**
     * 会员档位：0-无 1-轻享（免广告） 2-畅厨（免广告 + 分类菜谱不限）。语义见类注释
     */
    private Integer tier;
    /**
     * 到期时间；有效性 = expireAt 晚于当前时间，本表无 status 列
     */
    private LocalDateTime expireAt;
    /**
     * 首次支付时间（运营/留存分析用）。不参与首单优惠判定——那按 member_order 的 tier 维度查（§7.2）
     */
    private LocalDateTime firstPaidAt;
    /**
     * 最近支付时间
     */
    private LocalDateTime lastPaidAt;

    /**
     * 读时判定：按 expire_at 归零后的有效档位（docs/v12 §8.1）
     * <p>
     * ⚠️ 这是「档位透出」的唯一口径，{@code UserServiceImpl.info()} 与 {@code MemberServiceImpl.info()}
     * 都必须走它：两处各写一份归零判断，改一处漏一处就是「过期会员永久免广告」那条收入漏洞（§17 第 25 条）。
     * </p>
     *
     * @param now 判定时刻
     * @return 在期返回库里档位，过期或无档位返回 0
     */
    public int effectiveTier(LocalDateTime now) {
        if (tier == null || tier <= 0) {
            return 0;
        }
        if (expireAt == null || now == null || !expireAt.isAfter(now)) {
            return 0;
        }
        return tier;
    }

    /**
     * 是否在生效期
     *
     * @param now 判定时刻
     * @return 是否有效
     */
    public boolean isEffective(LocalDateTime now) {
        return effectiveTier(now) > 0;
    }
}
