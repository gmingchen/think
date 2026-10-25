package com.think.modules.wechat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 多媒体内容安全送审流水（media_checkAsync 异步结果落点；每次送审一行）
 * <p>
 * 送审时写入 trace_id 与资产定位（biz_type + biz_id + target），微信异步回调按 trace_id 反查该行、
 * 命中违规则据 biz_type 摘掉对应图片 / 重置头像
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "media_check", autoResultMap = true)
public class MediaCheckEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 资产类型：1-决策单图片 2-用户头像（MediaBizTypeEnum）
     */
    private Integer bizType;
    /**
     * 业务主键：决策单ID 或 用户ID
     */
    private Long bizId;
    /**
     * 资产定位：图片在 images 数组中的下标（头像为空）
     */
    private Integer target;
    /**
     * 送审的图片URL（回调命中违规时按此URL精确移除，防止下标漂移误删）
     */
    private String mediaUrl;
    /**
     * 微信 media_checkAsync 返回的 trace_id（异步结果关联键）
     */
    private String traceId;
    /**
     * 内容发布者 openid
     */
    private String openid;
    /**
     * 场景值 1/2/3/4
     */
    private Integer scene;
    /**
     * 审核渠道（默认 wx）
     */
    private String provider;
    /**
     * 微信回调结论：pass/review/risky（未回调为 pending）
     */
    private String suggest;
    /**
     * 单次审核结果：0-待审 1-通过 2-不通过（ContentCheckResultEnum）
     */
    private Integer checkStatus;
    /**
     * 命中标签/明细
     */
    private String detail;
    /**
     * 审核时间（回调落库时间）
     */
    private LocalDateTime checkedAt;
}
