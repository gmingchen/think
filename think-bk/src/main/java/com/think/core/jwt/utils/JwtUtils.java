package com.think.core.jwt.utils;

import com.think.core.jwt.properties.JsonWebToken;
import com.think.core.jwt.properties.JsonWebTokenProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Json Web Token 工具类
 * @author 好市民
 */
@Component
public class JwtUtils {

    @Autowired
    private JsonWebToken jsonWebToken;

    /**
     * 生成 jwt
     * @param id ID
     * @param secret 秘钥
     * @param expire 过期时间（秒）
     * @return
     */
    public String generate(Long id, String secret, Long expire) {
        Map<String, Object> headerMap = new HashMap<>(2);
        headerMap.put("alg", "HS256");
        headerMap.put("typ", "JWT");
        // 荷载信息
        Map<String, Object> claimsMap = new HashMap<>(1);
        claimsMap.put("id", id);

        Date now = new Date();

        return Jwts.builder()
                .setHeader(headerMap)
                .setClaims(claimsMap)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expire * 1000))
                .setSubject(id + "")
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 生成 jwt
     * @param id ID
     * @return
     */
    public String generate(Long id) {
        JsonWebTokenProperties jwtProperties = jsonWebToken.getJwt();
        return generate(id, jwtProperties.getSecret(), jwtProperties.getExpire());
    }

    /**
     * 获取 载荷 信息
     * @param token jwt
     * @param secret 秘钥
     * @return
     */
    public Claims getClaims(String token, String secret) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 载荷 信息
     * @param token jwt
     * @return
     */
    public Claims getClaims(String token) {
        JsonWebTokenProperties jwtProperties = jsonWebToken.getJwt();
        return getClaims(token, jwtProperties.getSecret());
    }

    /**
     * 验证是否过期
     * @param token jwt
     * @param secret 秘钥
     * @return
     */
    public boolean validate(String token, String secret) {
        Claims claims = getClaims(token, secret);
        return claims != null && claims.getExpiration().after(new Date());
    }

    /**
     * 验证是否过期
     * @param token jwt
     * @return
     */
    public boolean validate(String token) {
        Claims claims = getClaims(token);
        return claims != null && claims.getExpiration().after(new Date());
    }
}
