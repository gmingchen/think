package com.dishes.modules.subscribe.service;

import com.dishes.common.enums.SubscribeTypeEnum;

import java.util.Map;

/**
 * 订阅消息通用发送服务
 * <p>
 * 把「按业务类型找模板 → 扣本地配额 → 调微信发送 → 记流水」这条固定链路收在一处，
 * 各业务方（当前只有会员到期提醒，将来可扩展 cook-order 等）只组 data、不用各自重复配额与日志逻辑。
 * </p>
 *
 * @author 好市民
 */
public interface SubscribeMessageService {

    /**
     * 给某个用户发一条订阅消息。
     * <p>
     * 全程「发不出不报错」：模板未配置、无 openid、无配额、微信返回失败，都只记日志/流水并返回 false，
     * 绝不抛异常——订阅消息是锦上添花的提醒，不该因为它打断调用方的主流程（如定时任务整轮、或购买流程）。
     * </p>
     *
     * @param userId 接收人用户ID（用于扣本地配额）
     * @param openid 接收人 openid（微信投递地址；无登录态时由调用方回表取好传入，本服务不再依赖 user 表）
     * @param type   通知类型（据此从 subscribe_template 找模板ID）
     * @param data   模板字段数据，形如 {thing1:{value:...}, date2:{value:...}}
     * @param page   点击落地页（不带前导斜杠），可空
     * @return true-已成功发送；false-未发送（无配置/无配额/发送失败）
     */
    boolean send(Long userId, String openid, SubscribeTypeEnum type, Map<String, Object> data, String page);
}
