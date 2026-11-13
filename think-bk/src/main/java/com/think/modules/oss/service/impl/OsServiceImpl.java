package com.think.modules.oss.service.impl;

import com.think.modules.oss.model.vo.StorageTokenVO;
import com.think.modules.oss.service.OsService;
import com.think.modules.oss.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 对象存储对外统一入口实现（委托底层厂商 SPI {@link StorageService}）
 * @author 好市民
 */
@Service("osService")
public class OsServiceImpl implements OsService {

    @Autowired
    private StorageService storageService;

    @Override
    public StorageTokenVO getUploadToken() {
        return storageService.getUploadToken();
    }

    @Override
    public String getUrl(String key) {
        return storageService.getUrl(key);
    }

    @Override
    public void delete(String key) {
        storageService.delete(key);
    }
}
