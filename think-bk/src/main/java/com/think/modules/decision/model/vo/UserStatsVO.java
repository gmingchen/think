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
}
