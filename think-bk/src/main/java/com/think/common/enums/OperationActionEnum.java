package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作审计类型（对应 operation_log.action）
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum OperationActionEnum {
    /**
     * 操作类型
     */
    REPORT_HANDLE("REPORT_HANDLE", "举报处理"),
    GRP_DISSOLVE("GRP_DISSOLVE", "群解散"),
    DECISION_CLOSE("DECISION_CLOSE", "决策单关闭"),
    DECISION_DELETE("DECISION_DELETE", "决策单删除"),
    CHECK_HANDLE("CHECK_HANDLE", "审核处置"),
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
