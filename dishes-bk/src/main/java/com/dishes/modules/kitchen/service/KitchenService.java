package com.dishes.modules.kitchen.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.kitchen.entity.KitchenEntity;
import com.dishes.modules.kitchen.model.dto.KitchenCreateDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUpdateDTO;
import com.dishes.modules.kitchen.model.vo.KitchenStatsVO;
import com.dishes.modules.kitchen.model.vo.KitchenVO;

import java.util.List;

/**
 * 厨房
 * @author 好市民
 */
public interface KitchenService extends IServiceX<KitchenEntity> {

    /**
     * 查询列表
     * @return 列表
     */
    List<KitchenVO> getList();

    /**
     * 查询厨房信息
     * @param kitchenId 厨房ID
     * @return 厨房信息
     */
    KitchenVO info(Long kitchenId);

    /**
     * 创建厨房
     * @param dto 请求参数
     * @return 厨房ID
     */
    Long create(KitchenCreateDTO dto);

    /**
     * 为用户创建默认厨房
     * @param userId 用户ID
     * @return 厨房ID
     */
    Long createDefault(Long userId);

    /**
     * 更新厨房
     * @param dto 参数
     */
    void update(KitchenUpdateDTO dto);

    /**
     * 删除厨房
     * @param id 厨房ID
     */
    void delete(Long id);

    /**
     * 查询厨房统计数据
     * @param kitchenId 厨房ID
     * @return 统计数据
     */
    KitchenStatsVO getStats(Long kitchenId);

    /**
     * 看视频扩容菜谱上限（+1，仅厨师长/管厨）
     * @param kitchenId 厨房ID
     * @return 扩容后的厨房信息
     */
    KitchenVO expandDish(Long kitchenId);

    /**
     * 看视频扩容分类上限（+1，仅厨师长/管厨）
     * @param kitchenId 厨房ID
     * @return 扩容后的厨房信息
     */
    KitchenVO expandCategory(Long kitchenId);

}