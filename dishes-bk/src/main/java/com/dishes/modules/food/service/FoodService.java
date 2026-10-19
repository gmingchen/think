package com.dishes.modules.food.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.food.entity.FoodEntity;
import com.dishes.modules.food.model.dto.FoodCreateDTO;
import com.dishes.modules.food.model.dto.FoodDeleteDTO;
import com.dishes.modules.food.model.dto.FoodUpdateDTO;
import com.dishes.modules.food.model.vo.FoodVO;

import java.util.List;
import java.util.Map;

/**
 * 食材
 * @author 好市民
 */
public interface FoodService extends IServiceX<FoodEntity> {

    /**
     * 查询食材列表
     * @param kitchenId 厨房ID
     * @return 食材列表
     */
    List<FoodVO> listVO(Long kitchenId);

    /**
     * 新增食材
     * @param dto 食材信息
     * @return 食材ID
     */
    Long create(FoodCreateDTO dto);

    /**
     * 编辑食材
     * @param dto 食材信息
     */
    void update(FoodUpdateDTO dto);

    /**
     * 删除食材
     * @param dto 食材信息
     */
    void delete(FoodDeleteDTO dto);

    /**
     * 按名称（归一化）批量匹配厨房内食材：已有复用并使用次数+1，没有则新建
     * 收藏菜谱、AI 荐菜采纳等「跨来源按名落库」场景共用此口径
     * @param kitchenId 目标厨房ID
     * @param names 食材名称列表（允许重复，每次出现均计一次使用）
     * @param userId 新建时的创建者用户ID
     * @return 归一化名称 -> 食材ID
     */
    Map<String, Long> matchOrCreateByNames(Long kitchenId, List<String> names, Long userId);
}