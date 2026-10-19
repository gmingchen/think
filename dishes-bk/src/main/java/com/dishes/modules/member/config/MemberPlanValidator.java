package com.dishes.modules.member.config;

import com.dishes.modules.member.entity.MemberPlanEntity;
import com.dishes.modules.member.mapper.MemberPlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 套餐表启动校验（docs/v12 §9.1）
 * <p>
 * 表驱动之后「改数据不改代码」的自由度来自 {@code member_plan}，风险也来自它——一行填错就是钱上的错。
 * 所以把校验收在启动阶段，让问题在 JAR 起不来那一刻暴露，而不是在用户下单那一刻。
 * </p>
 * <p>
 * ⚠️ 一条必须知道的连带后果：本类会让<b>没建表或没插种子的库直接起不来</b>，
 * 连 {@code /auth/login} 都一起挂掉。这不是 bug，是 §8.4 上线顺序（建表 → 插种子，道具 ID 已随种子带入 → 部署 JAR）
 * 故意设的闸门；但部署前请务必先在目标库执行 {@code docs/v12/厨菜记v12-增量-会员.sql}。
 * </p>
 * <p>
 * 分工（2026-10-11 随支付通道解耦调整）：本类只查<b>表数据</b>；「凭证齐不齐」
 * 属配置自身，已移到通道侧的 {@code XpayPropertiesValidator}。
 * 同年同日第二次调整：{@code member_plan.env} 整列已删（后台实页证实沙箱与现网共用同一套道具 ID，
 * 「线上版本」只是开发版本发布后的只读镜像，价格不能独立编辑）；env 后续也整体改为按 profile 现算（{@code XpayEnv}），
 * 本类不再读任何 env 配置，也不得重新引入按环境筛套餐的逻辑。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class MemberPlanValidator {

    /**
     * 基线 6 个 SKU：表里必须**包含**这六个 code。
     * 是「包含」而不是「正好 6 行」——多出的 pro_half 之类不得报错，否则等于把 v12.2 加半年付的路堵回发版
     */
    private static final Set<String> BASELINE_CODES = new HashSet<>(Arrays.asList(
            "lite_month", "lite_quarter", "lite_year", "pro_month", "pro_quarter", "pro_year"));
    /**
     * 周期白名单：6 为半年付预留位，本版不插该行，但插了也不能报错
     */
    private static final Set<Integer> ALLOWED_MONTHS = new HashSet<>(Arrays.asList(1, 3, 6, 12));
    /**
     * 档位白名单：只有 1 轻享与 2 畅厨，加第三档时同步改这里与 §6.3 的权益规则
     */
    private static final Set<Integer> ALLOWED_TIERS = new HashSet<>(Arrays.asList(
            MemberPlanEntity.TIER_LITE, MemberPlanEntity.TIER_PRO));
    /**
     * 唯一允许带首单价的 SKU（§3.2：只留一个钩子，首单判定只涉一个档位）
     */
    private static final String FIRST_PRICE_CODE = "pro_month";

    @Autowired
    private MemberPlanMapper memberPlanMapper;

    @PostConstruct
    public void validate() {
        // 不按 env 筛：member_plan 已无 env 列，道具只有一套，整表就是当前生效的那一份价目
        List<MemberPlanEntity> plans = memberPlanMapper.selectList(null);
        if (plans == null || plans.isEmpty()) {
            throw new IllegalStateException("启动校验失败：member_plan 里没有任何套餐行，"
                    + "请先执行 docs/v12/厨菜记v12-增量-会员.sql 插入种子数据（§8.3 不做「表空回落 yml」的双头设计）");
        }

        List<String> problems = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        int missingProductId = 0;
        for (MemberPlanEntity plan : plans) {
            codes.add(plan.getCode());
            if (plan.getPrice() == null || plan.getPrice() <= 0) {
                problems.add("price 必须大于 0（分）：" + describe(plan));
            }
            if (plan.getMonths() == null || !ALLOWED_MONTHS.contains(plan.getMonths())) {
                problems.add("months 必须属于 " + ALLOWED_MONTHS + "：" + describe(plan));
            }
            if (plan.getTier() == null || !ALLOWED_TIERS.contains(plan.getTier())) {
                problems.add("tier 必须属于 " + ALLOWED_TIERS + "：" + describe(plan));
            }
            // 首单钩子只允许一个 SKU。这一条按文档原样只告警：它不影响已填价格的对错，
            // 而发放校验拿的是 expected_price，多填的 first_price 不会走漏到钱上（§7.2 要点 3）
            if (plan.getFirstPrice() != null && !FIRST_PRICE_CODE.equals(plan.getCode())) {
                log.error("member_plan 告警：非 {} 的套餐填了 first_price，该值不会生效，建议清空：{}",
                        FIRST_PRICE_CODE, describe(plan));
            }
            if (Integer.valueOf(1).equals(plan.getEnabled()) && StringUtils.isBlank(plan.getProductId())) {
                missingProductId++;
            }
        }

        Set<String> absent = new HashSet<>(BASELINE_CODES);
        absent.removeAll(codes);
        if (!absent.isEmpty()) {
            problems.add("基线套餐缺失，表里查不到这些 code：" + absent);
        }

        if (!problems.isEmpty()) {
            // 宁起不来也不要带错价起来：带错价起来的后果是 -15013 拒单或用户以错误价格买到权益
            throw new IllegalStateException("member_plan 启动校验不通过：" + String.join("；", problems));
        }
        if (missingProductId > 0) {
            // 只告警不阻启动：置灰版发版时道具本来还没建，硬拦会把版本卡死；真正的兜底在 order/create 拒单（§7.2）
            // ⚠️ 而它的作用范围只到建单那一步：paysign 只读订单快照而不查本表，但快照里的 product_id 已在建单时保证非空
            log.error("member_plan 告警：有 {} 行 enabled=1 但 product_id 为空，这些套餐一放开付费就会全部拒单，"
                    + "请在 MP 后台建好道具后把 product_id 填上（与 code 同值）", missingProductId);
        }
        log.info("member_plan 启动校验通过，行数={}", plans.size());
    }

    private String describe(MemberPlanEntity plan) {
        return "code=" + plan.getCode() + ", id=" + plan.getId();
    }
}
