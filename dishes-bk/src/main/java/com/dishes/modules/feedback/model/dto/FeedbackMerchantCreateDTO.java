package com.dishes.modules.feedback.model.dto;

import com.dishes.common.enums.FeedbackBusinessTypeEnum;
import com.dishes.common.enums.FeedbackDishScaleEnum;
import com.dishes.common.enums.FeedbackWillingnessEnum;
import com.dishes.core.validator.constraints.Enum;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 商户需求反馈提交DTO（docs/v13 §4）
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class FeedbackMerchantCreateDTO {
    /**
     * 经营类型（选填）
     */
    @Enum(FeedbackBusinessTypeEnum.class)
    private Integer businessType;
    /**
     * 现用管理方式（多选，选填，具体取值范围在 Service 层按 FeedbackCurrentToolEnum 校验）
     */
    private List<Integer> currentTool;
    /**
     * 菜品规模（选填）
     */
    @Enum(FeedbackDishScaleEnum.class)
    private Integer dishScale;
    /**
     * 核心痛点描述（必填）
     */
    @NotBlank(message = "请填写核心痛点")
    @Size(max = 500, message = "核心痛点不能超过 500 字")
    private String painPoint;
    /**
     * 期望功能（必填）
     */
    @NotBlank(message = "请填写希望解决什么问题")
    @Size(max = 500, message = "期望功能不能超过 500 字")
    private String expectFeature;
    /**
     * 付费意愿（选填）
     */
    @Enum(FeedbackWillingnessEnum.class)
    private Integer willingness;
    /**
     * 选填微信号
     */
    @Size(max = 50, message = "微信号不能超过 50 个字符")
    private String contactWx;
    /**
     * 选填手机号
     */
    @Size(max = 20, message = "手机号格式不正确")
    private String contactPhone;
}
