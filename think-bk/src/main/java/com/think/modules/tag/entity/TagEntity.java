package com.think.modules.tag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 标签（预置主数据，圈子雏形）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "tag", autoResultMap = true)
public class TagEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 标签名称
     */
    private String name;
    /**
     * 展示排序
     */
    private Integer sort;
    /**
     * 状态：0-禁用 1-启用（禁用后不再可选，历史关联保留）
     */
    private Integer status;
}
