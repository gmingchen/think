package com.dishes.modules.dish.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.dish.entity.DishEntity;
import com.dishes.modules.dish.model.dto.DishBatchUpdateCategoryDTO;
import com.dishes.modules.dish.model.dto.DishCollectDTO;
import com.dishes.modules.dish.model.dto.DishCreateDTO;
import com.dishes.modules.dish.model.dto.DishDeleteDTO;
import com.dishes.modules.dish.model.dto.DishShoppingListDTO;
import com.dishes.modules.dish.model.dto.DishSortDTO;
import com.dishes.modules.dish.model.dto.DishUpdateDTO;
import com.dishes.modules.dish.model.vo.DishInfoVO;
import com.dishes.modules.dish.model.vo.DishShoppingListVO;
import com.dishes.modules.dish.model.vo.DishShareVO;
import com.dishes.modules.dish.model.vo.DishVO;

import java.util.List;

/**
 * 菜品
 * @author 好市民
 */
public interface DishService extends IServiceX<DishEntity> {

    /**
     * 查询菜品列表
     * @param kitchenId 厨房ID
     * @return 菜品列表
     */
    List<DishVO> list(Long kitchenId);

    /**
     * 查询指定厨房、指定分类下的菜谱列表（按 sort 升序）
     * @param kitchenId 厨房ID
     * @param categoryId 分类ID；为 null 表示“未分类”组（与排序作用域一致）
     * @return 菜品列表
     */
    List<DishVO> listByCategory(Long kitchenId, Long categoryId);

    /**
     * 查询拿手好菜（评分最高，最多5条）
     * @param kitchenId 厨房ID
     * @return 菜品列表
     */
    List<DishVO> best(Long kitchenId);

    /**
     * 查询菜品详情
     * @param id 菜品ID
     * @return 菜品详情
     */
    DishInfoVO info(Long id);

    /**
     * 采购清单：批量查询多道菜的食材/调料明细（聚合合并由前端完成）
     * @param dto 菜谱ID列表 / 烹饪订单ID列表，至少传一个
     * @return 按菜分组的食材调料明细（含份数）
     */
    List<DishShoppingListVO> shoppingList(DishShoppingListDTO dto);

    /**
     * 新增菜品
     * @param dto 菜品信息
     * @return 菜品ID
     */
    Long create(DishCreateDTO dto);

    /**
     * 编辑菜品
     * @param dto 菜品信息
     */
    void update(DishUpdateDTO dto);

    /**
     * 删除菜品
     * @param dto 菜品信息
     */
    void delete(DishDeleteDTO dto);

    /**
     * 批量修改分类
     * @param dto 批量修改参数
     */
    void batchUpdateCategory(DishBatchUpdateCategoryDTO dto);

    /**
     * 菜谱排序（同一厨房同一分类内，ids 为新顺序）
     * @param dto 排序参数
     */
    void sort(DishSortDTO dto);

    /**
     * 增加做过次数
     * @param dishIds 菜品ID列表
     */
    void incrementCookCount(List<Long> dishIds);

    /**
     * 获取菜谱分享信息
     * @param id 菜谱ID
     * @return 分享信息
     */
    DishShareVO shareInfo(Long id);

    /**
     * 收藏菜谱到目标厨房
     * @param dto 收藏参数
     */
    void collect(DishCollectDTO dto);
}