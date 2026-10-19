package com.dishes.common.utils;

import java.time.LocalDateTime;

/**
 * 星座工具类
 * @author 好市民
 */
public class ConstellationUtils {
    /**
     * 星座
     */
    public static final String[] CONSTELLATION = { "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座" };
    /**
     * 星座结束日期
     */
    public static final int[] CONSTELLATION_EDGE_DAY = { 20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22 };

    /**
     * 根据日期获取星座
     * @param dateTime
     * @return
     */
    public static String getConstellation(LocalDateTime dateTime) {
        int month = dateTime.getMonth().getValue() - 1;
        int day = dateTime.getDayOfMonth();
        if (day < CONSTELLATION_EDGE_DAY[month]) {
            month = month - 1;
        }
        if (month >= 0) {
            return CONSTELLATION[month];
        }
        return CONSTELLATION[11];
    }
}
