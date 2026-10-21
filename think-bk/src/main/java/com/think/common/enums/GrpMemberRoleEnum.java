package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 群成员角色（对应 grp_member.role）
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum GrpMemberRoleEnum implements EnumIntArray {
    /**
     * 角色
     */
    OWNER(1, "群主"),
    MEMBER(2, "成员"),
    ;

    /**
     * 类型值
     */
    private final Integer code;
    /**
     * 类型名
     */
    private final String message;
    /**
     * 枚举值数组
     */
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(GrpMemberRoleEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
