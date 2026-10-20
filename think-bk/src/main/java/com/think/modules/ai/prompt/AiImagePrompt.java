package com.think.modules.ai.prompt;

import com.think.common.utils.CollectionUtils;
import com.think.common.utils.StringUtils;
import com.think.modules.ai.model.dto.AiImageGenDTO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 封面生图提示词
 * <p>
 * 单一大 string 拼好传给云函数（image-proxy 只接受 prompt 字段，不像 LLM 有 messages 结构）。
 * 开头固定风格锚（俯拍/暖光/家常菜质感），保证全站封面调性统一；
 * 中间只带菜名/食材/做法/调料，防止模型生成的图片风格飘。一期不开放用户自定义风格选项。
 * </p>
 * @author 好市民
 */
public class AiImagePrompt {

    /**
     * prompt 里每组信息各取前 N 项，避免 token 膨胀与图片元素过载
     */
    private static final int MAX_ITEM = 6;

    /**
     * 单条步骤取首个分句后的兜底长度上限，防止无标点长句吃掉整段预算
     */
    private static final int MAX_STEP_LEN = 20;

    /**
     * 混元 HY-Image-3.0 对 prompt 的硬上限（500 字），超出会被服务端拒，拼装时逐步缩减保证不超
     */
    private static final int MAX_PROMPT_LEN = 500;

    /**
     * 风格硬性锚定：与全站暖系手账 UI 一致，避免模型出法式摆盘/黑暗料理等跑偏风格；
     * 「禁止画面内出现文字」是防模型自发乱码/招牌，右下角 AI 合规标识由云函数 footnote 服务端烧入，两者不冲突
     */
    private static final String STYLE_ANCHOR = "\n风格要求：45°侧拍或俯拍、自然光、暖色温、原木餐桌背景、白色或素色陶瓷盘/砂锅、"
            + "超写实家常菜质感、8k 高清；"
            + "禁止：画面内出现文字、招牌、乱码、假 logo、人物、塑料感、畸变、多手、卡通风格。";

    /**
     * 步骤分句分隔符（中英文常见句读）
     */
    private static final String CLAUSE_END = "[，。；、！,.;!]";

    private AiImagePrompt() {
    }

    /**
     * 拼装最终 prompt（菜名必留 + 可选增强段 + 风格锚），云函数直接接收此字符串
     * <p>
     * 段序即优先级（对画面贡献从高到低）：食材 → 做法 → 调料 → 分类；每组逐条试装，
     * 超出 500 字预算就放弃本组剩余条目。除菜名外均为「可要可不要」，与 DTO 校验（仅 name 必填）一致
     * </p>
     * @param dto 生图请求参数
     * @return 完整 prompt（保证 ≤ 500 字）
     */
    public static String build(AiImageGenDTO dto) {
        String prompt = buildUserContent(dto) + STYLE_ANCHOR;
        // 极端情况：菜名本身过长（DTO 已限 64 字，理论不可达），硬截不越上限
        return prompt.length() <= MAX_PROMPT_LEN ? prompt : StringUtils.sub(prompt, 0, MAX_PROMPT_LEN);
    }

    /**
     * 只含用户可控内容的 prompt 段（菜名 + 食材/做法/调料/分类），不带平台自有的风格锚
     * <p>
     * 供内容安全审核送审使用：审核范围 = 真正会驱动模型出图的用户输入
     * </p>
     * @param dto 生图请求参数
     * @return 用户输入拼成的 prompt 前段
     */
    public static String buildUserContent(AiImageGenDTO dto) {
        String head = "一张【" + StringUtils.trim(dto.getName()) + "】的高清美食摄影图。";
        StringBuilder body = new StringBuilder();
        appendGroup(body, head, "主要食材", itemTexts(dto.getFoods(), true));
        appendGroup(body, head, "做法", stepTexts(dto.getSteps()));
        appendGroup(body, head, "调料", itemTexts(dto.getSeasonings(), false));
        appendGroup(body, head, "菜品分类", singleTexts(dto.getCategoryHint()));
        return head + body;
    }

    /**
     * 一组信息逐条试装，输出「标签：a / b / c。」；受 MAX_ITEM 与 500 字总预算双重限制，装不下的条目整条丢弃
     */
    private static void appendGroup(StringBuilder body, String head, String label, List<String> values) {
        StringBuilder picked = new StringBuilder();
        int count = 0;
        for (String value : values) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (count >= MAX_ITEM) {
                break;
            }
            String joined = count > 0 ? picked + " / " + value : value;
            if ((head + body + label + "：" + joined + "。" + STYLE_ANCHOR).length() > MAX_PROMPT_LEN) {
                // 预算已尽：本组后面的条目只会更长，直接收口
                break;
            }
            picked.setLength(0);
            picked.append(joined);
            count++;
        }
        if (count > 0) {
            body.append(label).append("：").append(picked).append("。");
        }
    }

    /**
     * 食材/调料条目：withAmount=true 输出「名 量」，false 只输出名；空名过滤
     */
    private static List<String> itemTexts(List<AiImageGenDTO.FoodItem> items, boolean withAmount) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(i -> i != null && StringUtils.isNotBlank(i.getName()))
                .map(i -> {
                    String name = StringUtils.trim(i.getName());
                    if (withAmount && StringUtils.isNotBlank(i.getAmount())) {
                        return name + " " + StringUtils.trim(i.getAmount());
                    }
                    return name;
                })
                .collect(Collectors.toList());
    }

    /**
     * 步骤条目：只取每条的首个分句——生图需要的是「炒/炖/蒸/烤/炸」这类决定成菜形态的做法，
     * 完整步骤动辄上百字；取分句而不是定长截断，避免切出残句干扰模型
     */
    private static List<String> stepTexts(List<String> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            return Collections.emptyList();
        }
        return steps.stream()
                .filter(s -> StringUtils.isNotBlank(s))
                .map(s -> StringUtils.sub(firstClause(s), 0, MAX_STEP_LEN))
                .collect(Collectors.toList());
    }

    /**
     * 截取第一个标点之前的动作短句，如「番茄划十字用开水烫一下去皮切块，鸡蛋打散…」→「番茄划十字用开水烫一下去皮切块」
     */
    private static String firstClause(String step) {
        String trimmed = StringUtils.trim(step);
        return trimmed.split(CLAUSE_END, 2)[0].trim();
    }

    private static List<String> singleTexts(String text) {
        return StringUtils.isBlank(text) ? Collections.emptyList() : Collections.singletonList(StringUtils.trim(text));
    }
}
