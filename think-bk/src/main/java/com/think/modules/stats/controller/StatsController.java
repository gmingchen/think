package com.think.modules.stats.controller;

import com.think.common.pojo.Result;
import com.think.modules.stats.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运营数据统计（用户维度）
 * <p>
 * 无管理员登录：整组接口在 {@code SecurityConfig} 白名单里 permitAll，上来即可看数据。全部只读 GET。
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

    /**
     * 登录活跃排行：区间内用户登录天数榜 / 登录次数榜 / 单日峰值榜。
     * 只读，同样在 /stats/** permitAll 白名单内；start / end 为 yyyy-MM-dd，缺省近 30 天；limit 取数上限缺省 10。
     */
    @GetMapping("/login-rank")
    public Result<Map<String, Object>> loginRank(@RequestParam(required = false) String start,
                                                 @RequestParam(required = false) String end,
                                                 @RequestParam(required = false) Integer limit) {
        return Result.success(statsService.loginRank(start, end, limit));
    }
}
