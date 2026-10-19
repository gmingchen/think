package com.dishes.modules.cookreview.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dishes.core.mybatisplus.mapper.BaseMapperX;
import com.dishes.modules.cookreview.entity.CookReviewEntity;
import com.dishes.modules.cookreview.model.vo.CookReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 烹饪评价
 * @author 好市民
 */
@Mapper
public interface CookReviewMapper extends BaseMapperX<CookReviewEntity> {

    List<CookReviewVO> selectReviewList(Page<?> page, @Param("dishId") Long dishId);
}
