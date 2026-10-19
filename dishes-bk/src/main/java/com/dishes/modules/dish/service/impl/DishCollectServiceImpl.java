package com.dishes.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dishes.common.utils.CollectionUtils;
import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.dish.entity.DishCollectEntity;
import com.dishes.modules.dish.mapper.DishCollectMapper;
import com.dishes.modules.dish.service.DishCollectService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜谱收藏记录
 * @author 好市民
 */
@Service("dishCollectService")
public class DishCollectServiceImpl extends ServiceImplX<DishCollectMapper, DishCollectEntity> implements DishCollectService {

    /**
     * 重复收藏提示
     */
    private static final String REPEAT_COLLECT_MESSAGE = "这道菜已经收藏在该厨房中";

    @Override
    public List<Long> listCollectedKitchenIds(Long dishId) {
        // 按厨房维度统计（与唯一索引 uk_dish_kitchen 口径一致），
        // 同厨房的其他成员收藏过，当前用户也应看到「已收藏」，避免前端可点、后端拒绝的割裂
        LambdaQueryWrapper<DishCollectEntity> wrapper = new LambdaQueryWrapper<DishCollectEntity>()
                .select(DishCollectEntity::getKitchenId)
                .eq(DishCollectEntity::getDishId, dishId);
        return CollectionUtils.mapList(list(wrapper), DishCollectEntity::getKitchenId);
    }

    @Override
    public boolean collected(Long dishId, Long kitchenId) {
        LambdaQueryWrapper<DishCollectEntity> wrapper = new LambdaQueryWrapper<DishCollectEntity>()
                .eq(DishCollectEntity::getDishId, dishId)
                .eq(DishCollectEntity::getKitchenId, kitchenId);
        return count(wrapper) > 0;
    }

    @Override
    public void record(Long dishId, Long kitchenId, Long targetDishId) {
        DishCollectEntity entity = new DishCollectEntity()
                .setDishId(dishId)
                .setKitchenId(kitchenId)
                .setUserId(SecurityUtils.getLoginUserId())
                .setTargetDishId(targetDishId);
        try {
            baseMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发请求同时通过应用层校验时，由唯一索引兜底，抛出后整个收藏事务回滚
            throw new RunException(REPEAT_COLLECT_MESSAGE);
        }
    }
}
