package com.dishes.modules.category.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.category.entity.CategoryEntity;
import com.dishes.modules.category.model.dto.CategoryCreateDTO;
import com.dishes.modules.category.model.dto.CategoryDeleteDTO;
import com.dishes.modules.category.model.dto.CategorySortDTO;
import com.dishes.modules.category.model.dto.CategoryUpdateDTO;
import com.dishes.modules.category.model.vo.CategoryVO;

import java.util.List;

/**
 * 分类
 * @author 好市民
 */
public interface CategoryService extends IServiceX<CategoryEntity> {

    /**
     * 查询厨房下的分类列表
     * @param kitchenId 厨房ID
     * @return 分类列表
     */
    List<CategoryVO> list(Long kitchenId);

    /**
     * 新增分类
     * @param dto 请求参数
     * @return 分类ID
     */
    Long create(CategoryCreateDTO dto);

    /**
     * 更新分类
     * @param dto 请求参数
     */
    void update(CategoryUpdateDTO dto);

    /**
     * 删除分类
     * @param dto 请求参数
     */
    void delete(CategoryDeleteDTO dto);

    /**
     * 分类排序
     * @param dto 请求参数
     */
    void sort(CategorySortDTO dto);
}