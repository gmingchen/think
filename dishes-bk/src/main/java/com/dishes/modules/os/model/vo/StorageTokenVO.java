package com.dishes.modules.os.model.vo;

import lombok.Data;

import java.util.Map;

/**
 * 上传凭证（通用）
 * @author 好市民
 */
@Data
public class StorageTokenVO {

    /**
     * 上传凭证
     */
    private String token;

    /**
     * 提供商额外信息（如 domain、bucket、region 等）
     */
    private Map<String, String> extra;
}
