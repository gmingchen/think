package com.think.modules.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.think.common.enums.StatusEnum;
import com.think.common.utils.CollectionUtils;
import com.think.core.mybatisplus.service.ServiceImplX;
import com.think.modules.tag.convert.TagConvert;
import com.think.modules.tag.entity.TagEntity;
import com.think.modules.tag.mapper.TagMapper;
import com.think.modules.tag.model.dto.TagDTO;
import com.think.modules.tag.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签
 *
 * @author 好市民
 */
@Service("tagService")
public class TagServiceImpl extends ServiceImplX<TagMapper, TagEntity> implements TagService {

    @Override
    public List<TagDTO> listEnabled() {
        LambdaQueryWrapper<TagEntity> wrapper = new LambdaQueryWrapper<TagEntity>()
                .eq(TagEntity::getStatus, StatusEnum.ENABLE.getCode())
                .orderByAsc(TagEntity::getSort);
        return CollectionUtils.mapList(baseMapper.selectList(wrapper), TagConvert.INSTANCE::convert);
    }
}
