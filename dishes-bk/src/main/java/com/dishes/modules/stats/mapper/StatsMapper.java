package com.dishes.modules.stats.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 运营数据统计（无后台，替代 docs/stats/运营数据统计.sql 手工查表）
 * <p>
 * 全部为只读聚合查询，故不继承 BaseMapperX、不绑实体：结果统一用 Map / List&lt;Map&gt; 承载，
 * 列别名即前端 JSON 字段名（camelCase）。区间类查询带 start / end（yyyy-MM-dd，含首尾两天），
 * 快照类（累计 / 有效会员等）不带区间。所有查询均已过滤软删除（deleted = 0）。
 * </p>
 *
 * @author 好市民
 */
@Mapper
public interface StatsMapper {

    // ── 综合看板 ──────────────────────────────────────────────

    /** 核心指标卡片（快照） */
    Map<String, Object> overviewCards();

    /**
     * 累计走势基准线：截至区间起始日前一天（created_at &lt; start）的各指标全量累计，
     * 供 /stats/cumulative 做「基准 + 每日新增前缀和」，避免逐日全表扫描。
     */
    Map<String, Object> cumulativeBaseline(@Param("start") String start);

    // ── 一、用户 ──────────────────────────────────────────────

    Map<String, Object> userSummary(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> userNewTrend(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> userActiveTrend(@Param("start") String start, @Param("end") String end);

    /** 今日 / 昨日逐小时新增用户数（user.created_at 小时桶），无参：服务端自取 CURDATE()-1 ~ CURDATE()+1 */
    List<Map<String, Object>> userHourlyNew();

    /** 今日 / 昨日逐小时活跃用户数（user_login_log 小时桶内去重 user_id） */
    List<Map<String, Object>> loginHourlyActive();

    List<Map<String, Object>> genderDist();

    List<Map<String, Object>> topInviter(@Param("limit") int limit);

    /** 区间内用户登录天数排行（按 user_login_log 去重登录日聚合，取前 limit） */
    List<Map<String, Object>> loginRankByDays(@Param("start") String start, @Param("end") String end, @Param("limit") int limit);

    /** 区间内用户登录次数排行（按 user_login_log 总登录次数聚合，取前 limit） */
    List<Map<String, Object>> loginRankByCount(@Param("start") String start, @Param("end") String end, @Param("limit") int limit);

    /** 区间内用户单日登录峰值排行（先按人/天计次，再取每人最高一天的次数，取前 limit） */
    List<Map<String, Object>> loginRankByPeak(@Param("start") String start, @Param("end") String end, @Param("limit") int limit);

    // ── 二、厨房 ──────────────────────────────────────────────

    Map<String, Object> kitchenSummary(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> kitchenNewTrend(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> kitchenMemberDist();

    List<Map<String, Object>> topKitchens(@Param("limit") int limit);

    // ── 三、菜谱 ──────────────────────────────────────────────

    Map<String, Object> dishSummary(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> dishNewTrend(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> topDishByCook(@Param("limit") int limit);

    List<Map<String, Object>> topDishByCollect(@Param("limit") int limit);

    Map<String, Object> dishCoverRatio();

    // ── 四、烹饪订单 ──────────────────────────────────────────

    Map<String, Object> orderSummary(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> orderTrend(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> orderPeriodDist(@Param("start") String start, @Param("end") String end);

    // ── 五、评价 ──────────────────────────────────────────────

    Map<String, Object> reviewSummary(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> reviewTrend(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> ratingDist(@Param("start") String start, @Param("end") String end);

    // ── 六、会员 ──────────────────────────────────────────────

    Map<String, Object> memberSummary();

    Map<String, Object> memberIncome();

    List<Map<String, Object>> memberTrend(@Param("start") String start, @Param("end") String end);

    List<Map<String, Object>> memberPlanDist();

    Map<String, Object> memberRefund();

    /** 区间会员订单漏斗（建单时间口径）：各状态计数 + 已发放金额 */
    Map<String, Object> memberOrderSummary(@Param("start") String start, @Param("end") String end);

    /** 区间支付周期偏好：已发放订单按 months 分组 */
    List<Map<String, Object>> memberMonthsDist(@Param("start") String start, @Param("end") String end);
}
