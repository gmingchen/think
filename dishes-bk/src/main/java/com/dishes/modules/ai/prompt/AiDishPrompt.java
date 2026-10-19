package com.dishes.modules.ai.prompt;

import com.dishes.common.utils.CollectionUtils;
import com.dishes.common.utils.StringUtils;

import java.util.List;

/**
 * AI 荐菜提示词
 * <p>
 * system 固定输出 JSON schema 与硬约束；user 按勾选食材、粗筛候选、去重菜名拼装。
 * 一次调用产出两段（ranked 排序理由 + fresh 新菜谱），节省 token
 * </p>
 * @author 好市民
 */
public class AiDishPrompt {

    private AiDishPrompt() {
    }

    /**
     * 系统提示词：角色 + 严格 JSON 输出 + 约束
     * @param rankedMax 第一组（现有能做）最多展示条数（随人数推导）
     * @param freshCount 第二组（新菜谱）建议生成条数（随人数推导）
     * @return system 文本
     */
    public static String system(int rankedMax, int freshCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位经验丰富的家厨助手，擅长根据家里现有食材安排一顿饭。");
        sb.append("你必须只输出一个合法的 JSON 对象，禁止输出任何解释文字、markdown 代码块标记或前后缀。\n");
        sb.append("JSON 结构严格如下：\n");
        sb.append("{\n");
        sb.append("  \"ranked\": [ { \"dishId\": 数字, \"reason\": \"一句话推荐理由\" } ],\n");
        sb.append("  \"fresh\": [ {\n");
        sb.append("    \"name\": \"菜名\",\n");
        sb.append("    \"remark\": \"一句话简介\",\n");
        sb.append("    \"categoryHint\": \"分类名（如 家常菜/主食/汤羹/凉菜/烘焙，尽量从中选择）\",\n");
        sb.append("    \"foods\": [ { \"name\": \"食材名\", \"amount\": \"用量含单位如200克\", \"process\": \"处理方式如切段\", \"isOptional\": 0或1 } ],\n");
        sb.append("    \"seasonings\": [ { \"name\": \"调料名\", \"amount\": \"用量含单位如3瓣\", \"isOptional\": 0或1 } ],\n");
        sb.append("    \"steps\": [ \"步骤1描述\", \"步骤2描述\" ]\n");
        sb.append("  } ]\n");
        sb.append("}\n");

        sb.append("\nranked 规则：每行开头的 [ID:数字] 是该菜的 dishId，dishId 必须原样取自候选行的 ID，不得自己编号或编造；按合适程度排序，最多 ").append(rankedMax).append(" 道；reason 要口语化、可结合人数口味忌口，不得编造候选里不存在的信息。\n");

        sb.append("\nfresh 规则：\n");
        sb.append("1. 生成 ").append(freshCount).append(" 道全新菜谱，必须以【已勾选食材】为主要原料，可搭配少量常见辅料；\n");
        sb.append("2. 菜名不得与【现有菜谱】重复，做法要有新意、和候选菜谱明显不同；\n");
        sb.append("3. 严格遵守用户的人数、口味、忌口限制；\n");
        sb.append("4. 每道菜谱 steps 不超过 6 步，每步不超过 60 字；\n");
        sb.append("5. isOptional：核心食材填 0，可省略的填 1；\n");
        sb.append("6. 葱、姜、蒜、辣椒、花椒、八角、桂皮等辛香料一律归入 seasonings，不得写入 foods。\n");
        return sb.toString();
    }

    /**
     * 用户提示词
     * @param selectedFoodNames 勾选的食材名
     * @param peopleCount 人数（可空）
     * @param taste 口味偏好（可空）
     * @param avoid 忌口（可空）
     * @param candidateTexts 粗筛候选菜谱的展示文本行（含已有/缺少食材）
     * @param existingDishNames 厨房现有菜名（供 fresh 去重）
     * @return user 文本
     */
    public static String user(List<String> selectedFoodNames,
                              Integer peopleCount,
                              String taste,
                              String avoid,
                              List<String> candidateTexts,
                              List<String> existingDishNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("【已勾选食材】").append(joinOrNone(selectedFoodNames)).append("\n");

        StringBuilder condition = new StringBuilder();
        if (peopleCount != null && peopleCount > 0) {
            condition.append("，人数：").append(peopleCount).append(" 人");
        }
        if (StringUtils.isNotBlank(taste)) {
            condition.append("，口味：").append(StringUtils.trim(taste));
        }
        if (StringUtils.isNotBlank(avoid)) {
            condition.append("，忌口：").append(StringUtils.trim(avoid));
        }
        if (condition.length() > 0) {
            sb.append("【本餐要求】").append(condition.substring(1)).append("\n");
        }

        if (CollectionUtils.isNotEmpty(candidateTexts)) {
            sb.append("【候选菜谱（请从中排序）】\n");
            for (int i = 0; i < candidateTexts.size(); i++) {
                sb.append(i + 1).append(". ").append(candidateTexts.get(i)).append("\n");
            }
        }

        if (CollectionUtils.isNotEmpty(existingDishNames)) {
            sb.append("【现有菜谱（新菜名请避开）】").append(joinOrNone(existingDishNames)).append("\n");
        }

        sb.append("请根据以上信息输出 ranked 排序与 fresh 新菜谱。");
        return sb.toString();
    }

    private static String joinOrNone(List<String> list) {
        return CollectionUtils.isEmpty(list) ? "无" : StringUtils.join("、", list);
    }
}
