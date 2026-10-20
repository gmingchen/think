package com.think.modules.subscribe.controller;

import com.think.common.pojo.Result;
import com.think.core.repeat.annotation.Repeat;
import com.think.core.security.utils.SecurityUtils;
import com.think.modules.subscribe.model.dto.SubscribeSyncDTO;
import com.think.modules.subscribe.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订阅消息（docs/v9 订阅消息通知）
 * <p>
 * 目前只暴露一个 {@code /subscribe/sync}：前端授权成功后把可发次数记到后端。需登录态（配额记在当前用户身上），
 * 不在 SecurityConfig 白名单里，走默认鉴权即可。发送是服务端内部行为（定时任务等），不在此开接口。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/subscribe")
public class SubscribeController {

    @Autowired
    private SubscribeService subscribeService;

    /**
     * 同步订阅授权次数。
     * <p>
     * 加 {@code @Repeat}：sync 是累加语义，连点/网络重试导致重复提交会把配额多加，
     * 用防重放窗口挡住同一请求的短时重复（v9 注意事项 4「次数同步幂等性」）。
     * </p>
     *
     * @param dto 模板ID + 次数
     * @return 空
     */
    @Repeat
    @PostMapping("/sync")
    public Result<?> sync(@RequestBody @Validated SubscribeSyncDTO dto) {
        subscribeService.sync(SecurityUtils.getLoginUserId(), dto.getTemplateId(), dto.getCount());
        return Result.success();
    }
}
