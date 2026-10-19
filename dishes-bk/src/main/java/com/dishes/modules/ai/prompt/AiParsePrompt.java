package com.dishes.modules.ai.prompt;

/**
 * AI 文案识别提示词
 * <p>
 * system 固定输出 JSON schema 与硬约束；user 原样带入待解析文案。
 * 字段名与 {@link com.dishes.modules.ai.model.vo.AiParseVO} 完全对齐，便于直接反序列化。
 * </p>
 * @author 好市民
 */
public class AiParsePrompt {

    private AiParsePrompt() {
    }

    /**
     * 系统提示词：角色 + 严格 JSON 输出 + 约束
     * @return system 文本
     */
    public static String system() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位经验丰富的家厨助手，请把用户提供的菜谱做法文案解析为结构化 JSON。");
        sb.append("你必须只输出一个合法的 JSON 对象，禁止输出任何解释文字、markdown 代码块标记或前后缀。\n");
        sb.append("JSON 结构严格如下：\n");
        sb.append("{\n");
        sb.append("  \"name\": \"菜名\",\n");
        sb.append("  \"remark\": \"一句话简介（文案没有可留空字符串）\",\n");
        sb.append("  \"categoryHint\": \"分类名（如 家常菜/主食/汤羹/凉菜/烘焙/热菜，尽量从中选择；文案无则留空字符串）\",\n");
        sb.append("  \"foods\": [ { \"name\": \"食材名\", \"amount\": \"用量含单位如500克\", \"process\": \"处理方式如切块（无则空字符串）\", \"isOptional\": 0或1 } ],\n");
        sb.append("  \"seasonings\": [ { \"name\": \"调料名\", \"amount\": \"用量含单位如2勺\", \"isOptional\": 0或1 } ],\n");
        sb.append("  \"steps\": [ \"步骤1描述\", \"步骤2描述\" ]\n");
        sb.append("}\n");

        sb.append("\n规则：\n");
        sb.append("1. 忠实于文案内容，不得编造文案里没有的食材/调料/步骤；文案缺某项则对应数组可为空；\n");
        sb.append("2. 葱、姜、蒜、辣椒、花椒、八角、桂皮等辛香料一律归入 seasonings，不得写入 foods；\n");
        sb.append("3. isOptional：核心食材/必需调料填 0，可省略的填 1；\n");
        sb.append("4. steps 按文案顺序拆分，每步一条，保留原文动作描述、去除序号前缀；\n");
        sb.append("5. name 取菜名，若文案未明确菜名，用主料概括（如取不到具体菜名时用「主料+做法」命名）；\n");
        sb.append("6. 条数与长度硬上限（超出部分直接舍弃，不要凑字数）：foods 与 seasonings 各最多 20 项、steps 最多 20 条；"
                + "name 与每项 name 不超过 30 字，amount/process 不超过 20 字，remark 不超过 200 字，每条 step 不超过 500 字。");
        return sb.toString();
    }

    /**
     * 用户提示词：原样带入待解析文案
     * @param text 做法文案
     * @return user 文本
     */
    public static String user(String text) {
        StringBuilder sb = new StringBuilder();
        sb.append("【做法文案】\n");
        sb.append(text == null ? "" : text).append("\n");
        sb.append("请解析为上述 JSON。");
        return sb.toString();
    }
}
