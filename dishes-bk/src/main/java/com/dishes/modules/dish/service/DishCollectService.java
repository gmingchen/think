package com.dishes.modules.dish.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.dish.entity.DishCollectEntity;

import java.util.List;

/**
 * 菜谱收藏记录
 * @author 好市民
 */
public interface DishCollectService extends IServiceX<DishCollectEntity> {

    /**
     * 查询指定菜谱已被当前登录用户收藏到的厨房ID列表
     * @param dishId 源菜谱ID
     * @return 厨房ID列表，未收藏过则为空集合
     */
    List<Long> listCollectedKitchenIds(Long dishId);

    /**
     * 判断目标厨房是否已收藏过该菜谱
     * @param dishId 源菜谱ID
     * @param kitchenId 目标厨房ID
     * @return 布尔值
     */
    boolean collected(Long dishId, Long kitchenId);

    /**
     * 记录一次收藏关系
     * 并发场景下由唯一索引 uk_dish_kitchen 兜底，重复时抛出业务异常
     * @param dishId 源菜谱ID
     * @param kitchenId 目标厨房ID
     * @param targetDishId 收藏后生成的新菜谱ID
     */
    void record(Long dishId, Long kitchenId, Long targetDishId);
}
