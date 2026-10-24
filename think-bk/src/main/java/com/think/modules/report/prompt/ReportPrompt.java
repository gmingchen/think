package com.think.modules.report.prompt;

import com.think.modules.ai.client.LlmClient;
import com.think.modules.decision.entity.DecisionEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 决策报告 · AI 三思兜底 Prompt（§3.6）
 * <p>
 * 仅零评议截止时调用：让模型按「思危 / 思退 / 思变」给出分析 + 各备选项权衡，替代空报告。
 * 输出纯文本，作为 report.ai_content 存留，页面挂「AI 内容仅供参考」声明。
 * </p>
 *
 * @author 好市民
 */
public class ReportPrompt {

    private ReportPrompt() {
    }

    private static final String SYSTEM =
            "你是人生两难决策的分析助手「众三思」。当一份决策单没有人评议时，你需要给出结构化的三思参考，" +
                    "帮助发起人从「思危（风险与最坏结果）/ 思退（退路与止损）/ 思变（选择之后生活的改变）」三个角度权衡。" +
                    "要求：1) 客观中立，不替用户做决定；2) 针对给出的每个备选项分别点出关键权衡；3) 结尾提醒「仅供参考，最终决定权在你」；" +
                    "4) 使用简体中文，控制在 500 字以内，不要输出 Markdown 标题符号。";

    /**
     * 构造发送给 LLM 的消息列表（system + user）。
     *
     * @param decision 决策单实体（含 title / content / options）
     * @return messages
     */
    public static List<Map<String, Object>> buildMessages(DecisionEntity decision) {
        String optionsText = decision.getOptions() == null ? "" : decision.getOptions().stream()
                .collect(Collectors.joining(" / "));
        String userContent = String.format(
                "【决策标题】%s%n【两难处境】%s%n【备选项】%s%n请按思危、思退、思变给出分析，并对每个备选项做简要权衡。",
                decision.getTitle(), decision.getContent(), optionsText);

        List<Map<String, Object>> messages = new ArrayList<>(2);
        messages.add(LlmClient.message("system", SYSTEM));
        messages.add(LlmClient.message("user", userContent));
        return messages;
    }
}
