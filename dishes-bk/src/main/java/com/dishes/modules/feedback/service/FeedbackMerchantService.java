package com.dishes.modules.feedback.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.feedback.entity.FeedbackMerchantEntity;
import com.dishes.modules.feedback.model.dto.FeedbackMerchantCreateDTO;

/**
 * 商户需求反馈（docs/v13，需求验证用）
 * @author 好市民
 */
public interface FeedbackMerchantService extends IServiceX<FeedbackMerchantEntity> {

    /**
     * 提交商户合作意向反馈。
     * <p>
     * 需登录态，反馈绑定当前用户的 userId / openid，便于与厨房容量、扩容、会员开通数据交叉核对；
     * 自由文本字段（痛点 / 期望功能）落库前必须过 msgSecCheck，命中违规直接抛业务异常、不落库。
     * </p>
     *
     * @param dto 表单字段
     */
    void create(FeedbackMerchantCreateDTO dto);
}
