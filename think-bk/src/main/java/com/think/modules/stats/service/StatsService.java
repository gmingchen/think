package com.think.modules.stats.service;

import java.util.Map;

/**
 * 运营数据统计服务（用户维度）
 *
 * @author 好市民
 */
public interface StatsService {

    /** 综合看板核心指标（快照）：用户总量 / 今日新增 / 今日活跃 */
    Map<String, Object> overview();

    /** 今日 / 昨日逐小时新增用户数与活跃用户数（快照，服务端自取今昨两天） */
    Map<String, Object> todayHourly();

    /**
     * 累计走势：区间内每日的累计用户总量。
     * 口径：区间前基准全量累计 + 区间内每日新增前缀和。
     */
    Map<String, Object> cumulative(String start, String end);

    /** 用户统计：summary + 新增/活跃趋势 + 性别分布 + 邀请排行（取前 limit） */
    Map<String, Object> user(String start, String end, Integer limit);

    /** 登录活跃排行：区间内用户登录天数榜 / 登录次数榜 / 单日峰值榜（各取前 limit） */
    Map<String, Object> loginRank(String start, String end, Integer limit);
}
