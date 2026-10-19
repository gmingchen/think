package com.dishes.common.constant;

/**
 * redis 存储key常量
 * @author 好市民
 */
public class RedisConstant {
    /**
     * 重复提交存储字段
     */
    public static final String REPEAT = "repeat:";
    /**
     * 微信凭证
     */
    public static final String WECHAT_ACCESS_TOKEN = "accessToken";
    /**
     * token存储key
     */
    public static final String TOKEN = "token:";
    /**
     * 用户
     */
    public static final String USER = "user:";
    /**
     * 厨房邀请码
     */
    public static final String KITCHEN_INVITE = "kitchen:invite:";
    /**
     * AI 荐菜推荐结果暂存 key（采纳凭证）
     */
    public static final String AI_RECOMMEND = "ai:recommend:";
    /**
     * AI 荐菜每日限次计数 key
     */
    public static final String AI_RECOMMEND_COUNT = "ai:recommend:count:";
    /**
     * AI 文案识别每日限次计数 key（独立预算，不与荐菜挤占）
     */
    public static final String AI_PARSE_COUNT = "ai:parse:count:";
    /**
     * AI 封面生图每日限次计数 key（独立预算，不与荐菜/识别挤占）
     */
    public static final String AI_IMAGE_COUNT = "ai:image:count:";
}
