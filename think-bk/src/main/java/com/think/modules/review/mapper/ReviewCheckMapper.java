package com.think.modules.review.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.think.core.mybatisplus.mapper.BaseMapperX;
import com.think.modules.review.entity.ReviewCheckEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评议内容审核流水
 *
 * @author 好市民
 */
@InterceptorIgnore(dataPermission = "true")
@Mapper
public interface ReviewCheckMapper extends BaseMapperX<ReviewCheckEntity> {

}
