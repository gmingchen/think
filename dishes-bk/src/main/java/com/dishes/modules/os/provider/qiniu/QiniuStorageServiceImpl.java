package com.dishes.modules.os.provider.qiniu;

import com.dishes.modules.os.config.StorageProperties;
import com.dishes.modules.os.model.vo.StorageTokenVO;
import com.dishes.modules.os.service.StorageService;
import com.dishes.exception.RunException;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 七牛云存储实现
 * @author 好市民
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "qiniu", matchIfMissing = true)
public class QiniuStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(QiniuStorageServiceImpl.class);

    /**
     * 上传凭证有效期（秒）：7200 = 2小时
     */
    private static final long TOKEN_EXPIRE_SECONDS = 7200;

    @Autowired
    private Auth qiniuAuth;
    @Autowired
    private StorageProperties properties;
    @Autowired
    private BucketManager bucketManager;

    @Override
    public StorageTokenVO getUploadToken() {
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        String token = qiniuAuth.uploadToken(qiniu.getBucket(), null, TOKEN_EXPIRE_SECONDS, null);

        Map<String, String> extra = new HashMap<>(4);
        extra.put("domain", qiniu.getDomain());
        extra.put("bucket", qiniu.getBucket());
        extra.put("uploadUrl", resolveUploadUrl(qiniu.getRegion()));

        StorageTokenVO vo = new StorageTokenVO();
        vo.setToken(token);
        vo.setExtra(extra);
        return vo;
    }

    @Override
    public String getUrl(String key) {
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        String domain = qiniu.getDomain();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/" + key;
    }

    @Override
    public String uploadByte(String key, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new RunException("上传内容为空");
        }
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        Configuration conf = new Configuration(resolveRegion(qiniu.getRegion()));
        UploadManager uploadManager = new UploadManager(conf);
        // 限定 key 的上传凭证，服务端直传小程序码等二进制内容
        String token = qiniuAuth.uploadToken(qiniu.getBucket(), key);
        try {
            uploadManager.put(bytes, key, token);
        } catch (QiniuException e) {
            log.error("七牛云字节上传失败, key={}, code={}, error={}", key, e.code(), e.getMessage());
            throw new RunException("小程序码上传失败");
        }
        return getUrl(key);
    }

    /**
     * 根据区域配置解析七牛 Region
     */
    private Region resolveRegion(String region) {
        switch (region) {
            case "huabei":   return Region.huabei();
            case "huanan":   return Region.huanan();
            case "beimei":   return Region.beimei();
            case "xinjiapo": return Region.xinjiapo();
            case "huadong":
            default:         return Region.huadong();
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        try {
            bucketManager.delete(qiniu.getBucket(), key);
        } catch (QiniuException e) {
            log.warn("七牛云文件删除失败, key={}, code={}, error={}", key, e.code(), e.getMessage());
        }
    }

    @Override
    public void deleteByUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        String key = extractKey(url);
        if (key != null) {
            delete(key);
        }
    }

    /**
     * 从完整 URL 中提取七牛文件 key
     */
    private String extractKey(String url) {
        StorageProperties.Qiniu qiniu = properties.getQiniu();
        String domain = qiniu.getDomain();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        if (url.startsWith(domain)) {
            String key = url.substring(domain.length());
            // 去掉开头的 /
            return key.startsWith("/") ? key.substring(1) : key;
        }
        return null;
    }

    /**
     * 根据区域解析上传端点
     */
    private String resolveUploadUrl(String region) {
        switch (region) {
            case "huabei":   return "https://upload-z1.qiniup.com";
            case "huanan":   return "https://upload-z2.qiniup.com";
            case "beimei":   return "https://upload-na0.qiniup.com";
            case "xinjiapo": return "https://upload-as0.qiniup.com";
            case "huadong":
            default:         return "https://upload.qiniup.com";
        }
    }
}
