package com.dishes.modules.preset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.modules.preset.convert.PresetConvert;
import com.dishes.modules.preset.entity.PresetCategoryEntity;
import com.dishes.modules.preset.mapper.PresetCategoryMapper;
import com.dishes.modules.preset.model.vo.PresetItemVO;
import com.dishes.modules.preset.service.PresetCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 预设分类
 * @author 好市民
 */
@Service("presetCategoryService")
public class PresetCategoryServiceImpl extends ServiceImplX<PresetCategoryMapper, PresetCategoryEntity> implements PresetCategoryService {

    @Override
    public List<PresetItemVO> getList() {
        LambdaQueryWrapper<PresetCategoryEntity> wrapper = new LambdaQueryWrapper<PresetCategoryEntity>()
                .orderByAsc(PresetCategoryEntity::getSort);
        List<PresetCategoryEntity> entities = list(wrapper);
        return CollectionUtils.mapList(entities, PresetConvert.INSTANCE::convert);
    }

}
