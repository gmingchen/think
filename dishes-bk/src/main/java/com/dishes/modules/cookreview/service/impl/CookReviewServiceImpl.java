package com.dishes.modules.cookreview.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dishes.common.pojo.PageResult;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.modules.cookreview.convert.CookReviewConvert;
import com.dishes.modules.cookreview.entity.CookReviewEntity;
import com.dishes.modules.cookreview.mapper.CookReviewMapper;
import com.dishes.modules.cookreview.model.dto.CookReviewDishDTO;
import com.dishes.modules.cookreview.model.dto.CookReviewPageDTO;
import com.dishes.modules.cookreview.model.vo.CookReviewVO;
import com.dishes.modules.cookreview.service.CookReviewService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 烹饪评价
 * @author 好市民
 */
@Service("cookReviewService")
public class CookReviewServiceImpl extends ServiceImplX<CookReviewMapper, CookReviewEntity> implements CookReviewService {

    @Override
    public PageResult<CookReviewVO> page(CookReviewPageDTO dto) {
        Page<?> page = new Page<>(dto.getCurrent(), dto.getSize());
        List<CookReviewVO> list = baseMapper.selectReviewList(page, dto.getDishId());
        return new PageResult<>(page.getTotal(), page.getPages(), list)
                .setCurrent(page.getCurrent());
    }

    @Override
    public void saveBatch(Long cookOrderId, Long userId, List<CookReviewDishDTO> dishes) {
        if (CollectionUtils.isEmpty(dishes)) return;
        List<CookReviewEntity> entities = CollectionUtils.mapList(dishes, CookReviewConvert.INSTANCE::convert);
        entities.forEach(e -> e.setCookOrderId(cookOrderId).setUserId(userId));
        baseMapper.insertBatch(entities);
    }
}
