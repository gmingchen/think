package com.think.modules.recap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.think.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 决策复盘（发起人回填结局，可选）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "recap", autoResultMap = true)
public class RecapEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 决策单ID（唯一）
     */
    private Long decisionId;
    /**
     * 最终选择：options 下标（0-based）；-1=其他/未选
     */
    private Integer finalChoice;
    /**
     * 实际发生了什么
     */
    private String actualResult;
    /**
     * 回填时间
     */
    private LocalDateTime reflectedAt;
}
