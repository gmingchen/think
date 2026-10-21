package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作审计业务对象类型（对应 operation_log.biz_type）
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum OperationBizTypeEnum {
    /**
     * 业务对象类型
     */
    DECISION("DECISION", "决策单"),
    REVIEW("REVIEW", "评议"),
    GRP("GRP", "群组"),
    ABUSE_REPORT("ABUSE_REPORT", "举报"),
    TAG("TAG", "标签"),
    ;

    /**
     * 类型值（存库字符串）
     */
    private final String code;
    /**
     * 类型名
     */
    private final String message;
}
