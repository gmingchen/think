package com.think.modules.tag.service;

import com.think.core.mybatisplus.service.IServiceX;
import com.think.modules.tag.entity.TagEntity;
import com.think.modules.tag.model.dto.TagDTO;

import java.util.List;

/**
 * 标签
 *
 * @author 好市民
 */
public interface TagService extends IServiceX<TagEntity> {

    /**
     * 查询启用中的标签列表（按 sort 升序）
     * @return 标签列表
     */
    List<TagDTO> listEnabled();
}
