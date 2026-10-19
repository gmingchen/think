package com.dishes.modules.auth.model.res;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户登录响应参数
 * @author 好市民
 */
@Accessors(chain = true)
@Data
public class LoginResVO {
    /**
     * 当前用户 ID
     */
    private Long userId;
    /**
     * 生成的 Token
     */
    private String token;
    /**
     * Token 过期时间
     */
    private LocalDateTime expiredAt;
    /**
     * 微信会话密钥（v12）
     * <p>
     * 存在意义只有一个：随 {@code token:} 的值进 Redis，下单时取它算用户态签名
     * {@code signature = hex(hmac_sha256(session_key, signData))}（docs/v12 §2.3）。
     * 本字段不会随登录响应下发给客户端（{@code AuthServiceImpl.login()} 存完 Redis 就置空），
     * 前端拿到也什么都不用做——它自己拿不到 session_key，也不需要。
     * </p>
     */
    private String sessionKey;
}
