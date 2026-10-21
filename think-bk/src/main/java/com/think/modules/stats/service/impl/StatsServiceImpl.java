package com.think.modules.stats.service.impl;

import com.think.modules.stats.mapper.StatsMapper;
import com.think.modules.stats.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营数据统计服务实现（用户维度）
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
        Map<String, Map<String, Object>> userIdx = toDtIndex(statsMapper.userNewTrend(s, e));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", rangeMap(s, e));
        result.put("users", runningSum(axis, userIdx, "num", base.get("userBase")));
        result.put("userDailyNew", dailySeries(axis, userIdx, "num"));
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

    /** 榜单取数上限归一：null / 非正数回退默认 10，最大 1000 兜底防大结果集（前端选项只放 10/20/50/100） */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RANK_LIMIT;
        }
        return Math.min(limit, MAX_RANK_LIMIT);
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
}
