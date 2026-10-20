package com.think.common.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;

/**
 * String 工具类
 * @author 好市民
 */
public class StringUtils extends StrUtil {

    /**
     * 名称归一化，用于跨厨房的食材/调料/分类名称匹配
     * 规则：全角转半角 -> 去除所有空白字符（含首尾与中间） -> 英文统一小写 -> 剥离 emoji/装饰符号
     * 例：「 五花肉　」「五花肉」视为同一名称；「Bacon」「bacon」视为同一名称；「番茄🍅」「番茄」视为同一名称
     * @param name 原始名称
     * @return 归一化后的名称，入参为空时返回空串
     */
    public static String normalizeName(String name) {
        if (StrUtil.isBlank(name)) {
            return "";
        }
        return removeSymbols(StrUtil.cleanBlank(Convert.toDBC(name)).toLowerCase());
    }

    /**
     * 剥离 emoji 及装饰性符号（象形符号、变体选择符、零宽连接符、肤色/国旗修饰符等），
     * 保留中日韩文字、字母、数字及普通标点，使「番茄🍅」与「番茄」归一化为同一匹配键
     */
    private static String removeSymbols(String text) {
        int[] codePoints = text.codePoints()
                .filter(cp -> !isDecorativeSymbol(cp))
                .toArray();
        return new String(codePoints, 0, codePoints.length);
    }

    private static boolean isDecorativeSymbol(int cp) {
        int type = Character.getType(cp);
        return type == Character.OTHER_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || (cp >= 0x1F000 && cp <= 0x1FAFF)
                || (cp >= 0x2600 && cp <= 0x27BF)
                || (cp >= 0x2B00 && cp <= 0x2BFF)
                || (cp >= 0xFE00 && cp <= 0xFE0F)
                || cp == 0x200D
                || (cp >= 0x1F1E6 && cp <= 0x1F1FF)
                || (cp >= 0x1F3FB && cp <= 0x1F3FF);
    }
}
