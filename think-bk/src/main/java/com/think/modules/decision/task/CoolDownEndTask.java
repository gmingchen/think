package com.think.modules.decision.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.DecisionStatusEnum;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.decision.service.DecisionNotifyService;
import com.think.modules.report.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评议时限到期定时任务（§3.3、§3.6）
 * <p>
 * 扫描 status=1（评议中）且 cool_down_end_at 已到的决策单：生成决策报告（零评议走 AI 三思兜底），
 * 并向发起人下发一次性订阅提醒。逐单处理、单单失败不影响整轮（失败单据下次扫描重试）。
 * </p>
 * <p>
 * 本类自身无事务：generate 自带事务、返回即已提交，提醒发放在其后执行，不会在持锁区间里跑 HTTP。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class CoolDownEndTask {

    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private ReportService reportService;
    @Autowired
    private DecisionNotifyService decisionNotifyService;

    /**
     * 每分钟扫描一次到期决策单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanExpired() {
        List<DecisionEntity> expired = decisionMapper.selectList(new LambdaQueryWrapper<DecisionEntity>()
                .eq(DecisionEntity::getStatus, DecisionStatusEnum.REVIEWING.getCode())
                .isNotNull(DecisionEntity::getCoolDownEndAt)
                .le(DecisionEntity::getCoolDownEndAt, LocalDateTime.now()));
        for (DecisionEntity decision : expired) {
            try {
                reportService.generate(decision.getId());
                // 收口通知统一走决策通知服务（与满员收口/提前结束共用一份组字段逻辑）
                decisionNotifyService.remindEnded(decision.getId());
            } catch (Exception e) {
                log.error("评议时限到期处理失败，decisionId={}", decision.getId(), e);
            }
        }
    }
}
