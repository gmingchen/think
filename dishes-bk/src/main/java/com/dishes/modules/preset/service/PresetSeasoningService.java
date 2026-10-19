package com.dishes.modules.preset.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.preset.entity.PresetSeasoningEntity;
import com.dishes.modules.preset.model.vo.GroupVO;
import com.dishes.modules.preset.model.vo.PresetItemVO;

import java.util.List;

/**
 * 预设调料
 * @author 好市民
 */
public interface PresetSeasoningService extends IServiceX<PresetSeasoningEntity> {

    List<GroupVO<PresetItemVO>> getGroupedList();

}
