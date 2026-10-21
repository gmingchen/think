package com.think.modules.tag.model.dto;

import lombok.Data;

/**
 * 标签信息
 *
 * @author 好市民
 */
@Data
public class TagDTO {
    /**
     * 标签ID
     */
    private Long id;
    /**
     * 标签名称
     */
    private String name;
    /**
     * 展示排序
     */
    private Integer sort;
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
}
