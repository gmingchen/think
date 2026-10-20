package com.think.modules.os.provider.qiniu;

import com.think.modules.os.config.StorageProperties;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 七牛云 Bean 配置（仅当 storage.type=qiniu 时生效）
 * @author 好市民
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "qiniu", matchIfMissing = true)
public class QiniuConfig {

    @Bean
    public Auth qiniuAuth(StorageProperties properties) {
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        return Auth.create(qiniu.getAccessKey(), qiniu.getSecretKey());
    }

    @Bean
    public UploadManager uploadManager(StorageProperties properties) {
        Region region = resolveRegion(properties.getQiniu().getRegion());
        com.qiniu.storage.Configuration cfg = new com.qiniu.storage.Configuration(region);
        return new UploadManager(cfg);
    }

    @Bean
    public BucketManager bucketManager(StorageProperties properties) {
        Region region = resolveRegion(properties.getQiniu().getRegion());
        com.qiniu.storage.Configuration cfg = new com.qiniu.storage.Configuration(region);
        return new BucketManager(qiniuAuth(properties), cfg);
    }

    private Region resolveRegion(String region) {
        switch (region) {
            case "huabei":
                return Region.huabei();
            case "huanan":
                return Region.huanan();
            case "beimei":
                return Region.beimei();
            case "xinjiapo":
                return Region.xinjiapo();
            case "huadong":
            default:
                return Region.huadong();
        }
    }
}
