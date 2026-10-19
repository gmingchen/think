package com.dishes.core.jwt.properties;

import lombok.Data;

/**
 * Json Web Token参数对象
 * @author 好市民
 */
@Data
public class JsonWebTokenProperties {
    /**
     * token 键值
     */
    private String key;
    /**
     * 秘钥
     */
    private String secret;
    /**
     * 过期是时间（秒）
     */
    private Long expire;
}
