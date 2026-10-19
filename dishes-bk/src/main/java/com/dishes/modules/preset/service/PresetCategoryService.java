package com.dishes.modules.preset.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.preset.entity.PresetCategoryEntity;
import com.dishes.modules.preset.model.vo.PresetItemVO;

import java.util.List;

/**
 * 预设分类
 * @author 好市民
 */
public interface PresetCategoryService extends IServiceX<PresetCategoryEntity> {

    List<PresetItemVO> getList();

}
