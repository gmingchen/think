package com.think.core.jwt.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 好市民
 */
@Data
@Component
@ConfigurationProperties("json-web-token")
public class JsonWebToken {
    private JsonWebTokenProperties jwt;
}
