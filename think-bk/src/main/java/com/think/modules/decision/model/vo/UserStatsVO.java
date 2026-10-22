package com.think.modules.decision.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 我的统计（个人中心统计区，§4.4，均实时统计不冗余）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class UserStatsVO {
    /**
     * 我发起的决策单数
     */
    private Integer createdCount;
    /**
     * 我参与评议的次数
     */
    private Integer reviewedCount;
    /**
     * 我的决策单累计收获的评议数
     */
    private Integer receivedCount;
    /**
     * 我已完成复盘的决策单数（status = 已复盘）
     */
    private Integer recapCount;
    /**
     * 结束评议（已截止 + 已复盘）单据的平均评议完成率，百分比整数部分（0-100）；无此类单据时为 null
     */
    private Integer reviewAvgRate;
}
