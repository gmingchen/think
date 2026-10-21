package com.think.modules.tag.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.tag.entity.TagEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface TagMapper extends BaseMapperX<TagEntity> {

}
