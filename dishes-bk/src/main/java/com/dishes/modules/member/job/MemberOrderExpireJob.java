package com.dishes.modules.member.job;

import com.dishes.modules.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时未支付订单关闭作业（docs/v12 §7.2 第二段末）
 * <p>
 * 这是本项目<b>第一个</b> {@code @Scheduled} 作业（{@code @EnableScheduling} 早在 DishesApplication 上开着，
 * 只是之前一直没人用）。它只干一件事：把过了 {@code expire_at} 还是 status=0 的单批量置 3 并留痕原因。
 * </p>
 * <p>
 * ⚠️ 为什么还要它——取签名那一步已经会就地拒签并关过期单了：那条路只覆盖「有人再来点一次」的单，
 * 用户杀了进程、断网、看了价又反悔的那些单不会自己关。反过来只做本作业也不行：每 5 分钟一轮，
 * 过期到下一轮之间仍能签出去付款，过期就形同虚设。两个入口合起来才把窗口闭合。
 * </p>
 * <p>
 * ⚠️ 它不怕和发放并发：批量 UPDATE 的条件里带 {@code status = 0}，正在发放的单会抢先进入 1。
 * 也不怕多实例重复执行：同一批行第二次扫就是 0 行。至于 {@code member.enabled} 为 false 时它照样跑——
 * 置灰只是不开新单，已有的死单更需要清。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class MemberOrderExpireJob {

    @Autowired
    private MemberService memberService;

    /**
     * 每 5 分钟扫一轮。用 fixedDelay 而不是 cron：上一轮没跑完就不会叠下一轮
     * （本机 MySQL 单条 UPDATE 走 {@code idx_status_created}，量级是个位数，但没必要赌重叠）。
     * 首轮延后 1 分钟，避开启动阶段的 {@code member_plan} 校验与连接池预热
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void closeExpiredOrders() {
        try {
            memberService.closeExpiredOrders();
        } catch (Exception e) {
            // 自己兜住：作业失败不该把后续调度带走，但也不能静默——关了单却没人知道和没关一样糟
            log.error("超时未支付订单关闭作业执行失败", e);
        }
    }
}
