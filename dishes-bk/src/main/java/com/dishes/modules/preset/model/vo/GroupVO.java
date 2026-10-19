package com.dishes.modules.preset.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 分组VO
 * @author 好市民
 */
@Data
public class GroupVO<T> {
    /**
     * 分组名称
     */
    private String name;
    /**
     * 分组数据
     */
    private List<T> list;
}
