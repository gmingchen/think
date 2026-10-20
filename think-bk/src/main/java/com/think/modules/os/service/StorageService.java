package com.think.modules.os.service;

import com.think.modules.os.model.vo.StorageTokenVO;

/**
 * 对象存储通用接口
 * <p>
 * 新增存储提供商时，实现此接口并注册为 Spring Bean 即可。
 * </p>
 * @author 好市民
 */
public interface StorageService {

    /**
     * 获取上传凭证
     * @return 上传凭证信息
     */
    StorageTokenVO getUploadToken();

    /**
     * 获取文件完整访问 URL
     * @param key 文件在存储空间中的 key
     * @return 完整 URL
     */
    String getUrl(String key);

    /**
     * 服务端直传字节数组到存储空间
     * @param key 文件在存储空间中的 key
     * @param bytes 文件字节内容
     * @return 文件完整访问 URL
     */
    String uploadByte(String key, byte[] bytes);

    /**
     * 删除文件
     * @param key 文件在存储空间中的 key
     */
    void delete(String key);

    /**
     * 根据完整 URL 删除文件
     * @param url 文件完整访问 URL
     */
    void deleteByUrl(String url);
}
