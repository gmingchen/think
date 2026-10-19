package com.dishes.modules.stats.service;

import java.util.Map;

/**
 * 运营数据统计服务
 *
 * @author 好市民
 */
public interface StatsService {

    /** 综合看板核心指标（快照） */
    Map<String, Object> overview();

    /** 今日 / 昨日逐小时新增用户数与活跃用户数（快照，服务端自取今昨两天） */
    Map<String, Object> todayHourly();

    /**
     * 累计走势：区间内每日的各指标累计总量（用户 / 厨房 / 菜谱 / 订单 / 评价 / 会员开通人次 / 收入）。
     * 口径：区间前基准全量累计 + 区间内每日新增前缀和，会员取「累计开通人次」（单调）而非有效会员。
     */
    Map<String, Object> cumulative(String start, String end);

    /** 用户统计：summary + 新增/活跃趋势 + 性别分布 + 邀请排行（取前 limit） */
    Map<String, Object> user(String start, String end, Integer limit);

    /** 登录活跃排行（v14）：区间内用户登录天数榜 / 登录次数榜 / 单日峰值榜（各取前 limit） */
    Map<String, Object> loginRank(String start, String end, Integer limit);

    /** 厨房统计：summary + 成员数分布 + 活跃厨房 TOP（取前 limit） */
    Map<String, Object> kitchen(String start, String end, Integer limit);

    /** 菜谱统计：summary + 新增趋势 + 热门/收藏 TOP（各取前 limit）+ 封面占比 */
    Map<String, Object> dish(String start, String end, Integer limit);

    /** 订单统计：summary + 趋势 + 时段分布 */
    Map<String, Object> order(String start, String end);

    /** 评价统计：summary + 趋势 */
    Map<String, Object> review(String start, String end);

    /** 会员统计：summary + 收入 + 开通趋势 + 套餐分布 + 退款 */
    Map<String, Object> member(String start, String end);
}
