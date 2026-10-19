package com.dishes.modules.os.service;

import com.dishes.modules.os.model.vo.StorageTokenVO;

/**
 * 对象存储（对外统一入口）
 * @author 好市民
 */
public interface OsService {

    /**
     * 获取上传凭证
     * @return 上传凭证信息
     */
    StorageTokenVO getUploadToken();

    /**
     * 获取文件完整访问 URL
     * @param key 文件 key
     * @return 完整 URL
     */
    String getUrl(String key);

    /**
     * 删除文件
     * @param key 文件 key
     */
    void delete(String key);
}
