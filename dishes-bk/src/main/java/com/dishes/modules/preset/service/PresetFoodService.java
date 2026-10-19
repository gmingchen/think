package com.dishes.modules.preset.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.preset.entity.PresetFoodEntity;
import com.dishes.modules.preset.model.vo.GroupVO;
import com.dishes.modules.preset.model.vo.PresetItemVO;

import java.util.List;

/**
 * 预设食材
 * @author 好市民
 */
public interface PresetFoodService extends IServiceX<PresetFoodEntity> {

    List<GroupVO<PresetItemVO>> getGroupedList();

}
