package com.dishes.modules.member.job;

import com.dishes.modules.member.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会员到期提醒作业：每天早上 9:00 给「3 天后到期」的在期会员各发一条订阅消息（续费提醒）。
 * <p>
 * 用 cron 而不是 fixedDelay：本作业语义就是「每天定点跑一次」，不是「跑完隔一会再跑」。
 * 9:00 是刻意选的白天时段——太早发订阅消息像骚扰，太晚错过当天续费决策。
 * </p>
 * <p>
 * ⚠️ 它依赖用户此前授权攒下的一次性订阅配额（前端点「开通」时弹 {@code requestSubscribeMessage}）。
 * 没有配额的用户微信直接返回 43101、消息不投递，服务端无法强推——这不是 bug，是订阅消息的机制。
 * 所以「发了但没人收到」通常是配额不足，去 service 的 warn 日志按 errcode 分辨即可。
 * </p>
 * <p>
 * ⚠️ 不拿 {@code member.enabled} 当闸门：那是「能不能买新单」的开关，与「提醒已有会员续费」无关。
 * 且窗口天然去重（每人一年只命中一次「今天 + 3 天」），多实例重复执行最坏是同一人同一天收到两次，
 * 但当前单机部署且一次性配额本就有限，不额外加分布式锁。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class MemberExpireRemindJob {

    @Autowired
    private MemberService memberService;

    /**
     * 每天 09:00:00 执行一轮。异常自己兜住：作业失败不该把后续调度带走，但也不能静默
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void remindExpiringMembers() {
        try {
            memberService.remindExpiringMembers();
        } catch (Exception e) {
            log.error("会员到期提醒作业执行失败", e);
        }
    }
}
