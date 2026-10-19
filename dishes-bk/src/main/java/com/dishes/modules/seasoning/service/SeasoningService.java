package com.dishes.modules.seasoning.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.seasoning.entity.SeasoningEntity;
import com.dishes.modules.seasoning.model.dto.SeasoningCreateDTO;
import com.dishes.modules.seasoning.model.dto.SeasoningDeleteDTO;
import com.dishes.modules.seasoning.model.dto.SeasoningUpdateDTO;
import com.dishes.modules.seasoning.model.vo.SeasoningVO;

import java.util.List;
import java.util.Map;

/**
 * 调料
 * @author 好市民
 */
public interface SeasoningService extends IServiceX<SeasoningEntity> {

    /**
     * 查询调料列表
     * @param kitchenId 厨房ID
     * @return 调料列表
     */
    List<SeasoningVO> listVO(Long kitchenId);

    /**
     * 新增调料
     * @param dto 调料信息
     * @return 调料ID
     */
    Long create(SeasoningCreateDTO dto);

    /**
     * 编辑调料
     * @param dto 调料信息
     */
    void update(SeasoningUpdateDTO dto);

    /**
     * 删除调料
     * @param dto 调料信息
     */
    void delete(SeasoningDeleteDTO dto);

    /**
     * 按名称（归一化）批量匹配厨房内调料：已有复用并使用次数+1，没有则新建
     * 收藏菜谱、AI 荐菜采纳等「跨来源按名落库」场景共用此口径
     * @param kitchenId 目标厨房ID
     * @param names 调料名称列表（允许重复，每次出现均计一次使用）
     * @param userId 新建时的创建者用户ID
     * @return 归一化名称 -> 调料ID
     */
    Map<String, Long> matchOrCreateByNames(Long kitchenId, List<String> names, Long userId);
}