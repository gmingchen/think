package com.think.modules.os.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储配置
 * <p>
 * storage.type 控制使用哪个提供商，当前支持：qiniu
 * </p>
 * @author 好市民
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 当前使用的存储提供商（qiniu / aliyun / tencent）
     */
    private String type = "qiniu";

    /**
     * 七牛云配置
     */
    private Qiniu qiniu = new Qiniu();

    @Data
    public static class Qiniu {
        /**
         * AccessKey（AK）
         */
        private String accessKey;
        /**
         * SecretKey（SK）
         */
        private String secretKey;
        /**
         * 存储空间名
         */
        private String bucket = "mx-think";
        /**
         * 存储区域（huadong/huabei/huanan/beimei/xinjiapo）
         */
        private String region = "huadong";
        /**
         * 外链域名
         */
        private String domain;
    }
}
