package com.think.modules.decision.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.DecisionStatusEnum;
import com.think.common.enums.SubscribeTypeEnum;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.report.service.ReportService;
import com.think.modules.subscribe.service.SubscribeMessageService;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评议时限到期定时任务（§3.3、§3.6）
 * <p>
 * 扫描 status=1（评议中）且 cool_down_end_at 已到的决策单：生成决策报告（零评议走 AI 三思兜底），
 * 并向发起人下发一次性订阅提醒。逐单处理、单单失败不影响整轮（失败单据下次扫描重试）。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class CoolDownEndTask {

    /**
     * 报告落地页（不带前导斜杠，订阅消息点击跳转）
     */
    private static final String REPORT_PAGE = "subpackages/decision/report?id=";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private ReportService reportService;
    @Autowired
    private UserService userService;
    @Autowired
    private SubscribeMessageService subscribeMessageService;

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
                remind(decision);
            } catch (Exception e) {
                log.error("评议时限到期处理失败，decisionId={}", decision.getId(), e);
            }
        }
    }

    /**
     * 到期订阅提醒：无配额/无 openid 时发送服务内部静默跳过
     */
    private void remind(DecisionEntity decision) {
        UserDTO user = userService.info(decision.getUserId());
        if (user == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>(4);
        data.put("thing1", abbreviate(decision.getTitle(), 20));
        data.put("time2", decision.getCoolDownEndAt() == null ? "" : decision.getCoolDownEndAt().format(TIME_FORMATTER));
        data.put("thing3", "评议时限已结束，点击查看决策报告");
        subscribeMessageService.send(decision.getUserId(), user.getOpenid(),
                SubscribeTypeEnum.COOL_DOWN_END, data, REPORT_PAGE + decision.getId());
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
