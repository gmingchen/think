package com.dishes.modules.subscribe.service;

/**
 * 订阅消息配额（记账侧）
 * <p>
 * 只管「某用户对某模板还剩几次可发」这件事：{@link #sync} 累加、{@link #consume} 扣减。
 * 真正「怎么发」在 {@link SubscribeMessageService}，它发之前先来 consume 领一次发送权。
 * </p>
 *
 * @author 好市民
 */
public interface SubscribeService {

    /**
     * 授权同步：前端 {@code requestSubscribeMessage} 拿到 accept 后调此累加可发次数（幂等 upsert）。
     *
     * @param userId     当前登录用户ID
     * @param templateId 微信模板ID
     * @param count      本次增加的可发次数（通常 1）
     */
    void sync(Long userId, String templateId, int count);

    /**
     * 发送前扣一次配额。
     *
     * @param userId     接收人用户ID
     * @param templateId 微信模板ID
     * @return true-扣到了一次发送权，可以发；false-无配额，应静默跳过
     */
    boolean consume(Long userId, String templateId);
}
