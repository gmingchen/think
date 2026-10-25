package com.think.modules.wechat.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.wechat.entity.MediaCheckEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 多媒体内容安全送审流水
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface MediaCheckMapper extends BaseMapperX<MediaCheckEntity> {

}
