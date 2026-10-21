package com.think.modules.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 评议（一条有效、禁自评、先评后看）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "review", autoResultMap = true)
public class ReviewEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 所属决策单ID
     */
    private Long decisionId;
    /**
     * 评议人用户ID（!= 发起人，禁自评）
     */
    private Long userId;
    /**
     * 倾向备选项下标（0-based）；option 为 MySQL 保留字，需反引号
     */
    @TableField(value = "`option`")
    private Integer option;
    /**
     * 评议观点文字（不能为空）
     */
    private String content;
    /**
     * 评议审核闸门（仅公开档）：0-无需 1-待审 2-通过 3-不通过（CheckStatusEnum）
     */
    private Integer checkStatus;
}
