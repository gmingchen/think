package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应结果状态码枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    /**
     * 状态码
     */
    SUCCESS(0, "成功！"),
    ERROR(500, "系统异常，请联系管理员！"),
    VERIFICATION_ERROR(400, "参数校验异常！"),
    NOT_ALLOWED(401, "没有权限访问！"),
    NOT_FOUND(404, "路径不存在！"),
    METHOD_ERROR(405, "不支持该请求方法！"),
    NOT_LOGIN(4000, "还未登录，请先登录！"),
    TOKEN_EXPIRE(4001, "凭证已过期，请重新登录！"),
    DUPLICATE_KEY(4002, "数据库中已存在该记录！"),
    BAD_SQL(4003, "SQL异常！"),
    DATA_INTEGRITY_VIOLATION(4004, "数据完整性异常！"),
    LIMIT_REACHED(4005, "已达上限！"),
    // 虚拟支付签名所需的 sessionKey 取不到（旧版本登录快照无此字段、或 Redis 值已过期）：
    // 前端据此**静默重新登录并重签同一单号**，所以它必须是一个可判的代码而不是靠比对文案（§12.1 -15007）
    SESSION_KEY_EXPIRED(4006, "登录态已过期，请重试！"),
    RESUBMIT(4038, "操作太频繁,请稍后重试！"),
    WECHAT_REQUEST_ERROR(4047, "请求微信API异常！"),
    USER_NONEXISTENT(4048, "用户不存在！"),
    USER_DISABLE(4050, "用户已被禁用！"),
    NOT_EXIST(4017, "该记录不存在！"),
    ;

    /**
     * 状态码
     */
    private final Integer code;
    /**
     * 状态信息
     */
    private final String message;
}
