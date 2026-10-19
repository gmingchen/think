package com.dishes.modules.stats.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.stats.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运营数据统计（配套 dishes-admin 后台看板）
 * <p>
 * 无管理员登录：整组接口在 {@code SecurityConfig} 白名单里 permitAll，上来即可看数据，
 * 替代以往手工执行 docs/stats/运营数据统计.sql 的查表方式。全部只读 GET。
 * </p>
 * <p>
 * 除 {@code /overview}、{@code /today-hourly} 外均接收 start / end（yyyy-MM-dd）；缺省取近 30 天，口径见 {@link StatsService}。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statsService.overview());
    }

    /**
     * 今日 / 昨日逐小时新增与活跃用户数：快照类，无参数，服务端自取今昨两天。
     */
    @GetMapping("/today-hourly")
    public Result<Map<String, Object>> todayHourly() {
        return Result.success(statsService.todayHourly());
    }

    @GetMapping("/cumulative")
    public Result<Map<String, Object>> cumulative(@RequestParam(required = false) String start,
                                                  @RequestParam(required = false) String end) {
        return Result.success(statsService.cumulative(start, end));
    }

    @GetMapping("/user")
    public Result<Map<String, Object>> user(@RequestParam(required = false) String start,
                                            @RequestParam(required = false) String end,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(statsService.user(start, end, limit));
    }

    @GetMapping("/kitchen")
    public Result<Map<String, Object>> kitchen(@RequestParam(required = false) String start,
                                               @RequestParam(required = false) String end,
                                               @RequestParam(required = false) Integer limit) {
        return Result.success(statsService.kitchen(start, end, limit));
    }

    @GetMapping("/dish")
    public Result<Map<String, Object>> dish(@RequestParam(required = false) String start,
                                            @RequestParam(required = false) String end,
                                            @RequestParam(required = false) Integer limit) {
        return Result.success(statsService.dish(start, end, limit));
    }

    @GetMapping("/order")
    public Result<Map<String, Object>> order(@RequestParam(required = false) String start,
                                             @RequestParam(required = false) String end) {
        return Result.success(statsService.order(start, end));
    }

    @GetMapping("/review")
    public Result<Map<String, Object>> review(@RequestParam(required = false) String start,
                                              @RequestParam(required = false) String end) {
        return Result.success(statsService.review(start, end));
    }

    @GetMapping("/member")
    public Result<Map<String, Object>> member(@RequestParam(required = false) String start,
                                              @RequestParam(required = false) String end) {
        return Result.success(statsService.member(start, end));
    }

    /**
     * 登录活跃排行（v14）：区间内用户登录天数榜 / 登录次数榜 / 单日峰值榜。
     * 只读，同样在 /stats/** permitAll 白名单内；start / end 为 yyyy-MM-dd，缺省近 30 天；limit 取数上限缺省 10。
     */
    @GetMapping("/login-rank")
    public Result<Map<String, Object>> loginRank(@RequestParam(required = false) String start,
                                                 @RequestParam(required = false) String end,
                                                 @RequestParam(required = false) Integer limit) {
        return Result.success(statsService.loginRank(start, end, limit));
    }
}
