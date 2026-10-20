package com.think.modules.subscribe.service.impl;

import com.think.modules.subscribe.mapper.SubscribeMapper;
import com.think.modules.subscribe.service.SubscribeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 订阅消息配额（记账侧）实现
 * @author 好市民
 */
@Slf4j
@Service("subscribeService")
public class SubscribeServiceImpl implements SubscribeService {

    @Autowired
    private SubscribeMapper subscribeMapper;

    @Override
    public void sync(Long userId, String templateId, int count) {
        // 参数兜底：脏数据不该进账本，但也不值得为一次授权失败抛错打断用户，静默返回即可
        if (userId == null || StringUtils.isBlank(templateId) || count <= 0) {
            return;
        }
        subscribeMapper.upsertAdd(userId, templateId, count);
    }

    @Override
    public boolean consume(Long userId, String templateId) {
        if (userId == null || StringUtils.isBlank(templateId)) {
            return false;
        }
        return subscribeMapper.casConsume(userId, templateId) > 0;
    }
}
