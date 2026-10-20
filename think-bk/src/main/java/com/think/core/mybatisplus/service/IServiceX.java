package com.think.core.mybatisplus.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author 好市民
 */
public interface IServiceX<T> extends IService<T> {

    /**
     * 批量插入 删除
     * 已存在的不进行操作
     * @param primaryId 主要的ID
     * @param modifiedIds 要修改的ID数组
     * @param selectEqColumnFn 查询条件的列
     * @param modifiedIdMap 筛选修改列的函数
     * @param insertMap 插入的函数
     * @param deleteInColumnFn 删除条件的列
     */
    void saveOrRemoveBatch(
            Serializable primaryId,
            Collection<?> modifiedIds,
            SFunction<T, ?> selectEqColumnFn,
            Function<T, Serializable> modifiedIdMap,
            Function<Serializable, T> insertMap,
            SFunction<T, ?> deleteInColumnFn);

    /**
     * 校验是否存在
     * @param id ID
     * @return 布尔值
     */
    boolean validateIsExists(Serializable id);

    /**
     * 校验是否存在
     * @param id ID
     * @return
     */
    T validateExists(Serializable id);
}
