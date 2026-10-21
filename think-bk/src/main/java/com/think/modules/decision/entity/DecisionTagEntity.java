package com.think.modules.decision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 决策单标签关联（多对多中间表，仅公开档，<=5）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "decision__tag", autoResultMap = true)
public class DecisionTagEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 决策单ID
     */
    private Long decisionId;
    /**
     * 标签ID
     */
    private Long tagId;
}
