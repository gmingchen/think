package com.dishes.modules.feedback.controller;

import com.dishes.common.pojo.Result;
import com.dishes.core.repeat.annotation.Repeat;
import com.dishes.modules.feedback.model.dto.FeedbackMerchantCreateDTO;
import com.dishes.modules.feedback.service.FeedbackMerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户需求反馈（docs/v13，需求验证用）
 * <p>
 * 目前只暴露一个 {@code /feedback/merchant}：「我的」页「商户合作」入口提交意向表单。
 * 需登录态（反馈要绑定当前用户，便于与厨房容量 / 会员开通数据交叉核对），
 * 不在 SecurityConfig 白名单里，走默认鉴权即可。本版为验证性需求，不建「反馈列表」查询接口，
 * 若后续判定不立项商户版，本模块可整体删除（分包 + 接口 + 表），不影响 user / member 等核心模块。
 * </p>
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackMerchantController {

    @Autowired
    private FeedbackMerchantService feedbackMerchantService;

    /**
     * 提交商户合作意向反馈。
     * <p>
     * 加 {@code @Repeat}：连点 / 网络重试不产生重复记录，避免污染样本统计（模式同 {@code SubscribeController.sync}）。
     * </p>
     *
     * @param dto 表单字段
     * @return 空
     */
    @Repeat
    @PostMapping("/merchant")
    public Result<?> create(@RequestBody @Validated FeedbackMerchantCreateDTO dto) {
        feedbackMerchantService.create(dto);
        return Result.success();
    }
}
