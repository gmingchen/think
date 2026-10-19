package com.dishes.modules.cookorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.exception.RunException;
import com.dishes.modules.cookorder.convert.CookOrderConvert;
import com.dishes.modules.cookorder.entity.CookOrderDishEntity;
import com.dishes.modules.cookorder.mapper.CookOrderDishMapper;
import com.dishes.modules.cookorder.model.dto.CookOrderDishDTO;
import com.dishes.modules.cookorder.model.vo.CookOrderDishVO;
import com.dishes.modules.cookorder.service.CookOrderDishService;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.service.DishService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 烹饪订单菜品关联
 * @author 好市民
 */
@Service("cookOrderDishService")
public class CookOrderDishServiceImpl extends ServiceImplX<CookOrderDishMapper, CookOrderDishEntity> implements CookOrderDishService {

    @Autowired
    private DishService dishService;

    @Override
    public List<CookOrderDishVO> list(Long cookOrderId) {
        LambdaQueryWrapper<CookOrderDishEntity> wrapper = new LambdaQueryWrapper<CookOrderDishEntity>()
                .eq(CookOrderDishEntity::getCookOrderId, cookOrderId);
        List<CookOrderDishEntity> entities = baseMapper.selectList(wrapper);
        return CollectionUtils.mapList(entities, CookOrderConvert.INSTANCE::convertDishVO);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void create(Long cookOrderId, List<CookOrderDishDTO> dishes) {
        if (CollectionUtils.isEmpty(dishes)) {
            return;
        }
        // 批量查询菜品信息（用于快照）
        List<Long> dishIds = CollectionUtils.mapList(dishes, CookOrderDishDTO::getDishId);
        Map<Long, DishEntity> dishMap = dishService.listByIds(dishIds).stream()
                .collect(Collectors.toMap(DishEntity::getId, Function.identity()));
        // 构建关联实体
        List<CookOrderDishEntity> entities = new ArrayList<>();
        for (CookOrderDishDTO dto : dishes) {
            DishEntity dish = dishMap.get(dto.getDishId());
            if (dish == null) {
                throw new RunException("菜谱不存在：" + dto.getDishId());
            }
            entities.add(new CookOrderDishEntity()
                    .setCookOrderId(cookOrderId)
                    .setDishId(dto.getDishId())
                    .setDishName(dish.getName())
                    .setDishCover(dish.getCover())
                    .setQuantity(dto.getQuantity()));
        }
        saveBatch(entities);
    }

    @Transactional(rollbackFor = RunException.class)
    @Override
    public void deleteByCookOrderId(Long cookOrderId) {
        LambdaQueryWrapper<CookOrderDishEntity> wrapper = new LambdaQueryWrapper<CookOrderDishEntity>()
                .eq(CookOrderDishEntity::getCookOrderId, cookOrderId);
        baseMapper.delete(wrapper);
    }
}
