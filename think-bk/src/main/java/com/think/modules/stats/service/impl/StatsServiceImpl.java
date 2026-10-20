package com.think.modules.stats.service.impl;

import com.think.modules.stats.mapper.StatsMapper;
import com.think.modules.stats.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营数据统计服务实现
 * <p>
 * 只做编排：区间归一 + 趋势按天补零对齐。真正的聚合都在 {@link StatsMapper} 的 XML 里。
 * 缺省区间为「近 30 天（含今日）」；start / end 均按 yyyy-MM-dd 处理，任一为空则各自回退。
 * </p>
 *
 * @author 好市民
 */
@Service
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 登录榜单默认取数与上限（前端可选 10/20/50/100，后端兼容任意正数并封顶） */
    private static final int DEFAULT_RANK_LIMIT = 10;
    private static final int MAX_RANK_LIMIT = 1000;

    /** 一天的小时数，逐小时走势补齐用 */
    private static final int HOURS_OF_DAY = 24;

    @Autowired
    private StatsMapper statsMapper;

    @Override
    public Map<String, Object> overview() {
        return statsMapper.overviewCards();
    }

    @Override
    public Map<String, Object> todayHourly() {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DAY);
        String yesterdayStr = today.minusDays(1).format(DAY);
        int currentHour = LocalTime.now().getHour();

        Map<String, Map<String, Object>> newIdx = toHourIndex(statsMapper.userHourlyNew());
        Map<String, Map<String, Object>> activeIdx = toHourIndex(statsMapper.loginHourlyActive());

        List<Map<String, Object>> series = new ArrayList<>();
        for (int h = 0; h < HOURS_OF_DAY; h++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", h);
            row.put("yesterdayNew", pick(hourRow(newIdx, yesterdayStr, h), "num", 0));
            row.put("yesterdayActive", pick(hourRow(activeIdx, yesterdayStr, h), "num", 0));
            // 今日未到的小时置 null：前端折线自然断开，不补 0 以免误读为「活跃归零」
            boolean reached = h <= currentHour;
            row.put("todayNew", reached ? pick(hourRow(newIdx, todayStr, h), "num", 0) : null);
            row.put("todayActive", reached ? pick(hourRow(activeIdx, todayStr, h), "num", 0) : null);
            series.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today", todayStr);
        result.put("yesterday", yesterdayStr);
        result.put("series", series);
        return result;
    }

    @Override
    public Map<String, Object> cumulative(String start, String end) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        List<String> axis = buildAxis(s, e);

        Map<String, Object> base = statsMapper.cumulativeBaseline(s);

        // 各指标区间内每日新增（按天索引），与基准一起做前缀和得到每日累计总量
        Map<String, Map<String, Object>> userIdx = toDtIndex(statsMapper.userNewTrend(s, e));
        Map<String, Map<String, Object>> kitchenIdx = toDtIndex(statsMapper.kitchenNewTrend(s, e));
        Map<String, Map<String, Object>> dishIdx = toDtIndex(statsMapper.dishNewTrend(s, e));
        Map<String, Map<String, Object>> orderIdx = toDtIndex(statsMapper.orderTrend(s, e));
        Map<String, Map<String, Object>> reviewIdx = toDtIndex(statsMapper.reviewTrend(s, e));
        Map<String, Map<String, Object>> memberIdx = toDtIndex(statsMapper.memberTrend(s, e));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("users", runningSum(axis, userIdx, "num", base.get("userBase")));
        result.put("kitchens", runningSum(axis, kitchenIdx, "num", base.get("kitchenBase")));
        result.put("dishes", runningSum(axis, dishIdx, "num", base.get("dishBase")));
        result.put("orders", runningSum(axis, orderIdx, "total", base.get("orderBase")));
        result.put("reviews", runningSum(axis, reviewIdx, "num", base.get("reviewBase")));
        result.put("memberOpens", runningSum(axis, memberIdx, "num", base.get("memberOpenBase")));
        result.put("revenues", runningSumDecimal(axis, memberIdx, "revenue", base.get("revenueBase")));

        // 每日新增（非累计）：与上面累计同源于已查的 idx，不额外查库，供「每日增量」图复用
        result.put("userDailyNew", dailySeries(axis, userIdx, "num"));
        result.put("kitchenDailyNew", dailySeries(axis, kitchenIdx, "num"));
        result.put("dishDailyNew", dailySeries(axis, dishIdx, "num"));
        result.put("orderDailyNew", dailySeries(axis, orderIdx, "total"));
        result.put("reviewDailyNew", dailySeries(axis, reviewIdx, "num"));
        result.put("memberOpenDailyNew", dailySeries(axis, memberIdx, "num"));
        result.put("revenueDailyNew", dailySeriesDecimal(axis, memberIdx, "revenue"));
        return result;
    }

    @Override
    public Map<String, Object> user(String start, String end, Integer limit) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        int top = normalizeLimit(limit);
        List<String> axis = buildAxis(s, e);

        Map<String, Map<String, Object>> newIdx = toDtIndex(statsMapper.userNewTrend(s, e));
        Map<String, Map<String, Object>> activeIdx = toDtIndex(statsMapper.userActiveTrend(s, e));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d);
            row.put("newUsers", pick(newIdx.get(d), "num", 0));
            // 邀请来源下钻拆分：与 newUsers 同源于 userNewTrend，两者之和恒等于新增总量
            row.put("newInvitedUsers", pick(newIdx.get(d), "invitedNum", 0));
            row.put("newNormalUsers", pick(newIdx.get(d), "normalNum", 0));
            row.put("activeUsers", pick(activeIdx.get(d), "num", 0));
            trend.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("limit", top);
        result.put("summary", statsMapper.userSummary(s, e));
        result.put("trend", trend);
        result.put("gender", statsMapper.genderDist());
        result.put("topInviter", statsMapper.topInviter(top));
        return result;
    }

    @Override
    public Map<String, Object> loginRank(String start, String end, Integer limit) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        int top = normalizeLimit(limit);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("limit", top);
        result.put("byDays", statsMapper.loginRankByDays(s, e, top));
        result.put("byCount", statsMapper.loginRankByCount(s, e, top));
        result.put("byPeak", statsMapper.loginRankByPeak(s, e, top));
        return result;
    }

    /** 榜单取数上限归一：null / 非正数回退默认 10，最大 1000 兜底防大结果集（前端选项只放 10/20/50/100） */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RANK_LIMIT;
        }
        return Math.min(limit, MAX_RANK_LIMIT);
    }

    @Override
    public Map<String, Object> kitchen(String start, String end, Integer limit) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        int top = normalizeLimit(limit);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("limit", top);
        result.put("summary", statsMapper.kitchenSummary(s, e));
        result.put("memberDist", statsMapper.kitchenMemberDist());
        result.put("topKitchens", statsMapper.topKitchens(top));
        return result;
    }

    @Override
    public Map<String, Object> dish(String start, String end, Integer limit) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        int top = normalizeLimit(limit);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("limit", top);
        result.put("summary", statsMapper.dishSummary(s, e));
        result.put("trend", singleTrend(statsMapper.dishNewTrend(s, e), buildAxis(s, e), "num", "newDishes"));
        result.put("topCook", statsMapper.topDishByCook(top));
        result.put("topCollect", statsMapper.topDishByCollect(top));
        result.put("cover", statsMapper.dishCoverRatio());
        return result;
    }

    @Override
    public Map<String, Object> order(String start, String end) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        List<String> axis = buildAxis(s, e);

        Map<String, Map<String, Object>> idx = toDtIndex(statsMapper.orderTrend(s, e));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> row = idx.get(d);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("date", d);
            out.put("total", pick(row, "total", 0));
            out.put("done", pick(row, "done", 0));
            trend.add(out);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("summary", statsMapper.orderSummary(s, e));
        result.put("trend", trend);
        result.put("periodDist", statsMapper.orderPeriodDist(s, e));
        return result;
    }

    @Override
    public Map<String, Object> review(String start, String end) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        List<String> axis = buildAxis(s, e);

        Map<String, Map<String, Object>> idx = toDtIndex(statsMapper.reviewTrend(s, e));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> row = idx.get(d);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("date", d);
            out.put("num", pick(row, "num", 0));
            out.put("avgRating", pick(row, "avgRating", 0));
            trend.add(out);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("summary", statsMapper.reviewSummary(s, e));
        result.put("trend", trend);
        result.put("ratingDist", statsMapper.ratingDist(s, e));
        return result;
    }

    @Override
    public Map<String, Object> member(String start, String end) {
        String[] range = normalize(start, end);
        String s = range[0];
        String e = range[1];
        List<String> axis = buildAxis(s, e);

        Map<String, Map<String, Object>> idx = toDtIndex(statsMapper.memberTrend(s, e));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> row = idx.get(d);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("date", d);
            out.put("num", pick(row, "num", 0));
            out.put("revenue", pick(row, "revenue", 0));
            trend.add(out);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("summary", statsMapper.memberSummary());
        result.put("income", statsMapper.memberIncome());
        result.put("trend", trend);
        result.put("planDist", statsMapper.memberPlanDist());
        result.put("refund", statsMapper.memberRefund());
        result.put("orders", statsMapper.memberOrderSummary(s, e));
        result.put("monthsDist", statsMapper.memberMonthsDist(s, e));
        return result;
    }

    // ── 私有工具 ──────────────────────────────────────────────

    /** 归一化区间：都为空则取近 30 天（含今日）；单边为空则各回退 30 天 */
    private String[] normalize(String start, String end) {
        LocalDate today = LocalDate.now();
        LocalDate eDate = (end == null || end.isEmpty()) ? today : LocalDate.parse(end, DAY);
        LocalDate sDate = (start == null || start.isEmpty()) ? eDate.minusDays(29) : LocalDate.parse(start, DAY);
        if (sDate.isAfter(eDate)) {
            LocalDate tmp = sDate;
            sDate = eDate;
            eDate = tmp;
        }
        return new String[]{sDate.format(DAY), eDate.format(DAY)};
    }

    private List<String> buildAxis(String start, String end) {
        LocalDate s = LocalDate.parse(start, DAY);
        LocalDate e = LocalDate.parse(end, DAY);
        List<String> axis = new ArrayList<>();
        for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
            axis.add(d.format(DAY));
        }
        return axis;
    }

    /** 单指标趋势补零对齐到完整日期轴 */
    private List<Map<String, Object>> singleTrend(List<Map<String, Object>> rows, List<String> axis,
                                                   String valueKey, String outKey) {
        Map<String, Map<String, Object>> idx = toDtIndex(rows);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("date", d);
            out.put(outKey, pick(idx.get(d), valueKey, 0));
            trend.add(out);
        }
        return trend;
    }

    private Map<String, Map<String, Object>> toDtIndex(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> idx = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object dt = row.get("dt");
                if (dt != null) {
                    idx.put(String.valueOf(dt), row);
                }
            }
        }
        return idx;
    }

    /** 逐小时流水按「日期|小时」建索引，空集安全 */
    private Map<String, Map<String, Object>> toHourIndex(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> idx = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object dt = row.get("dt");
                Object hr = row.get("hr");
                if (dt != null && hr != null) {
                    idx.put(dt + "|" + hr, row);
                }
            }
        }
        return idx;
    }

    private Map<String, Object> hourRow(Map<String, Map<String, Object>> idx, String date, int hour) {
        return idx.get(date + "|" + hour);
    }

    private Object pick(Map<String, Object> row, String key, Object def) {
        if (row == null) {
            return def;
        }
        Object v = row.get(key);
        return v == null ? def : v;
    }

    private Map<String, Object> rangeMap(String start, String end) {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("start", start);
        range.put("end", end);
        return range;
    }

    /** 整数类累计：基准值 + 区间内每日新增的前缀和，逐日输出 {date, num} */
    private List<Map<String, Object>> runningSum(List<String> axis, Map<String, Map<String, Object>> idx,
                                                 String valueKey, Object base) {
        long acc = toLong(base);
        List<Map<String, Object>> out = new ArrayList<>();
        for (String d : axis) {
            acc += toLong(pick(idx.get(d), valueKey, 0));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d);
            row.put("num", acc);
            out.add(row);
        }
        return out;
    }

    /** 金额类累计（保留 2 位）：基准金额 + 区间内每日新增收入的前缀和，逐日输出 {date, amount} */
    private List<Map<String, Object>> runningSumDecimal(List<String> axis, Map<String, Map<String, Object>> idx,
                                                        String valueKey, Object base) {
        BigDecimal acc = toDecimal(base);
        List<Map<String, Object>> out = new ArrayList<>();
        for (String d : axis) {
            acc = acc.add(toDecimal(pick(idx.get(d), valueKey, 0)));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d);
            row.put("amount", acc.setScale(2, BigDecimal.ROUND_HALF_UP));
            out.add(row);
        }
        return out;
    }

    private long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    /** 每日新增序列（非累计），输出 {date, num} */
    private List<Map<String, Object>> dailySeries(List<String> axis, Map<String, Map<String, Object>> idx, String valueKey) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d);
            row.put("num", toLong(pick(idx.get(d), valueKey, 0)));
            out.add(row);
        }
        return out;
    }

    /** 每日新增金额序列（非累计，保留 2 位），输出 {date, amount} */
    private List<Map<String, Object>> dailySeriesDecimal(List<String> axis, Map<String, Map<String, Object>> idx, String valueKey) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String d : axis) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d);
            row.put("amount", toDecimal(pick(idx.get(d), valueKey, 0)).setScale(2, BigDecimal.ROUND_HALF_UP));
            out.add(row);
        }
        return out;
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(String.valueOf(v));
    }
}
