package com.dishes.modules.feedback.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 商户需求反馈（docs/v13，需求验证用，只写入不做前端展示）
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "feedback_merchant", autoResultMap = true)
public class FeedbackMerchantEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 提交用户ID
     */
    private Long userId;
    /**
     * 微信openid（便于回访定位）
     */
    private String openid;
    /**
     * 经营类型，见 FeedbackBusinessTypeEnum
     */
    private Integer businessType;
    /**
     * 现用管理方式，多选逗号分隔存储 FeedbackCurrentToolEnum 的 code
     */
    private String currentTool;
    /**
     * 菜品规模，见 FeedbackDishScaleEnum
     */
    private Integer dishScale;
    /**
     * 核心痛点描述（自由文本，落库前必须过 msgSecCheck）
     */
    private String painPoint;
    /**
     * 期望功能（自由文本，选填，落库前必须过 msgSecCheck）
     */
    private String expectFeature;
    /**
     * 付费意愿，见 FeedbackWillingnessEnum
     */
    private Integer willingness;
    /**
     * 选填微信号
     */
    private String contactWx;
    /**
     * 选填手机号
     */
    private String contactPhone;
}
