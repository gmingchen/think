package com.think.modules.decision.service.impl;

import com.think.common.enums.SubscribeTypeEnum;
import com.think.modules.decision.entity.DecisionEntity;
import com.think.modules.decision.mapper.DecisionMapper;
import com.think.modules.decision.service.DecisionNotifyService;
import com.think.modules.subscribe.service.SubscribeMessageService;
import com.think.modules.user.model.dto.UserDTO;
import com.think.modules.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 决策单收口通知实现
 * <p>
 * 只读回表 + 组装模板字段 + 交给通用发送服务，本身不开事务、不写业务表，
 * 因此可安全地被到期任务、评议提交、提前结束三处不同事务上下文的调用方复用。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Service("decisionNotifyService")
public class DecisionNotifyServiceImpl implements DecisionNotifyService {

    /**
     * 报告落地页（不带前导斜杠，订阅消息点击跳转）：必须与 app.json 分包真实路径一致
     * （root=subpackages/decision + pages/report/index），路径无效会导致微信投递失败或点开白屏
     */
    private static final String REPORT_PAGE = "subpackages/decision/pages/report/index?id=";

    /**
     * 「结束时间」（time2）格式：微信 time 类型字段要求到秒（官方示例 2020-12-25 12:30:00），
     * 缺秒会被判为数据格式错误（errcode 47003）导致整条消息发不出去
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 「投票标题」（thing1）截断长度：thing 类型字段上限 20 个汉字
     */
    private static final int TITLE_MAX = 20;

    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private SubscribeMessageService subscribeMessageService;

    @Override
    public void remindEnded(Long decisionId) {
        // 整段兜异常：本方法总在收口事务提交之后执行（afterCommit / 事务方法返回后），
        // 此时任何异常再冒泡都会把已成功的收口变成接口 500（数据已落库、提示却报错）；
        // 提醒只是锦上添花，坏了只能记日志，不能影响主流程结果
        try {
            doRemindEnded(decisionId);
        } catch (Exception e) {
            log.error("决策单收口提醒下发异常，decisionId={}", decisionId, e);
        }
    }

    private void doRemindEnded(Long decisionId) {
        DecisionEntity decision = decisionMapper.selectById(decisionId);
        if (decision == null) {
            return;
        }
        UserDTO user = userService.info(decision.getUserId());
        if (user == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>(4);
        data.put("thing1", abbreviate(decision.getTitle(), TITLE_MAX));
        // 不限时或提前结束的单据没有 cool_down_end_at，用实际收口时刻兜底：
        // time 类型字段留空同样会被微信判格式错误，整条消息发不出去
        LocalDateTime endAt = decision.getCoolDownEndAt() == null ? LocalDateTime.now() : decision.getCoolDownEndAt();
        data.put("time2", endAt.format(TIME_FORMATTER));
        // 文案对到期/满员/提前结束三种收口都成立，故不写「时限已结束」
        data.put("thing3", "评议已结束，点击查看决策报告");
        subscribeMessageService.send(decision.getUserId(), user.getOpenid(),
                SubscribeTypeEnum.COOL_DOWN_END, data, REPORT_PAGE + decisionId);
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
