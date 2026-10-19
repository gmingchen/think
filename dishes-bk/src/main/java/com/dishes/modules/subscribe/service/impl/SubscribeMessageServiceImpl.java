package com.dishes.modules.subscribe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.enums.SubscribeTypeEnum;
import com.dishes.modules.subscribe.entity.SubscribeLogEntity;
import com.dishes.modules.subscribe.entity.SubscribeTemplateEntity;
import com.dishes.modules.subscribe.mapper.SubscribeLogMapper;
import com.dishes.modules.subscribe.mapper.SubscribeTemplateMapper;
import com.dishes.modules.subscribe.service.SubscribeMessageService;
import com.dishes.modules.subscribe.service.SubscribeService;
import com.dishes.modules.wechat.model.WechatResponse;
import com.dishes.modules.wechat.service.AppletSubscribeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 订阅消息通用发送服务实现
 * @author 好市民
 */
@Slf4j
@Service("subscribeMessageService")
public class SubscribeMessageServiceImpl implements SubscribeMessageService {

    @Autowired
    private SubscribeTemplateMapper subscribeTemplateMapper;
    @Autowired
    private SubscribeLogMapper subscribeLogMapper;
    @Autowired
    private SubscribeService subscribeService;
    /**
     * 最底层的微信发送通道（凭证自愈 + 组装 touser/template_id/data/page 都在它里面）
     */
    @Autowired
    private AppletSubscribeService appletSubscribeService;

    @Override
    public boolean send(Long userId, String openid, SubscribeTypeEnum type, Map<String, Object> data, String page) {
        if (userId == null || type == null) {
            return false;
        }
        // 1. 按业务类型找微信模板ID：模板 ID 只存 subscribe_template，业务侧不硬编码（v9 注意事项 3）
        String templateId = resolveTemplateId(type);
        if (StringUtils.isBlank(templateId)) {
            log.warn("订阅消息类型 {} 未在 subscribe_template 配置模板ID，跳过发送", type.getCode());
            return false;
        }
        // 2. 无 openid 不发送，且要在扣配额之前判：否则白扣一次发送权却发不出去
        if (StringUtils.isBlank(openid)) {
            log.warn("用户 {} 无 openid，订阅消息 {} 无法投递，跳过（不扣配额）", userId, type.getCode());
            return false;
        }
        // 3. 扣本地配额：没配额（用户没授权或已用完）就静默跳过，不发也不报错
        if (!subscribeService.consume(userId, templateId)) {
            return false;
        }
        // 4. 调微信发送并记流水。consume 已扣，若这里抛异常配额不回退：
        //    微信发送是网络调用，异常多半是超时等不确定态，回退反而可能重发；宁可少发一次也不重复打扰
        int status;
        String message;
        boolean delivered;
        try {
            WechatResponse resp = appletSubscribeService.push(templateId, openid, data, page);
            delivered = resp != null && Integer.valueOf(0).equals(resp.getErrcode());
            status = delivered ? SubscribeLogEntity.STATUS_SUCCESS : SubscribeLogEntity.STATUS_FAIL;
            message = delivered ? null : (resp == null ? "无响应" : resp.getErrcode() + " " + resp.getErrmsg());
        } catch (Exception e) {
            delivered = false;
            status = SubscribeLogEntity.STATUS_FAIL;
            message = StringUtils.abbreviate(e.getMessage(), 480);
            log.error("订阅消息发送异常，userId={}，type={}", userId, type.getCode(), e);
        }
        writeLog(userId, templateId, type, status, message);
        return delivered;
    }

    /**
     * 按类型取模板ID；selectOne 走 MyBatis-Plus 自动带 deleted=0
     */
    private String resolveTemplateId(SubscribeTypeEnum type) {
        SubscribeTemplateEntity row = subscribeTemplateMapper.selectOne(
                new LambdaQueryWrapper<SubscribeTemplateEntity>()
                        .eq(SubscribeTemplateEntity::getType, type.getCode()));
        return row == null ? null : row.getTemplateId();
    }

    /**
     * 记发送流水。写流水本身失败绝不该影响发送结果，故吞掉异常只留日志
     */
    private void writeLog(Long userId, String templateId, SubscribeTypeEnum type, int status, String message) {
        try {
            SubscribeLogEntity logEntity = new SubscribeLogEntity()
                    .setUserId(userId)
                    .setTemplateId(templateId)
                    .setType(type.getCode())
                    .setStatus(status)
                    .setMessage(message);
            subscribeLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("订阅消息发送流水写入失败，userId={}，type={}", userId, type.getCode(), e);
        }
    }
}
