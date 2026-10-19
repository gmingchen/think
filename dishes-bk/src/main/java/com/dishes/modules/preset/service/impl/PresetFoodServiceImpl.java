package com.dishes.modules.preset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.modules.preset.convert.PresetConvert;
import com.dishes.modules.preset.entity.PresetFoodEntity;
import com.dishes.modules.preset.mapper.PresetFoodMapper;
import com.dishes.modules.preset.model.vo.GroupVO;
import com.dishes.modules.preset.model.vo.PresetItemVO;
import com.dishes.modules.preset.service.PresetFoodService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预设食材
 * @author 好市民
 */
@Service("presetFoodService")
public class PresetFoodServiceImpl extends ServiceImplX<PresetFoodMapper, PresetFoodEntity> implements PresetFoodService {

    @Override
    public List<GroupVO<PresetItemVO>> getGroupedList() {
        LambdaQueryWrapper<PresetFoodEntity> wrapper = new LambdaQueryWrapper<PresetFoodEntity>()
                .orderByAsc(PresetFoodEntity::getSort);
        List<PresetFoodEntity> all = list(wrapper);
        List<PresetItemVO> items = CollectionUtils.mapList(all, PresetConvert.INSTANCE::convert);
        Map<String, List<PresetItemVO>> grouped = items.stream()
                .collect(Collectors.groupingBy(PresetItemVO::getCategory));
        List<GroupVO<PresetItemVO>> result = new ArrayList<>();
        grouped.forEach((name, list) -> {
            GroupVO<PresetItemVO> vo = new GroupVO<>();
            vo.setName(name);
            vo.setList(list);
            result.add(vo);
        });
        return result;
    }

}
