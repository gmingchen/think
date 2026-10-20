package com.think.common.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体对象
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class BasePO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT, jdbcType = JdbcType.TIMESTAMP)
    private LocalDateTime createdAt;
    /**
     * 最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE, jdbcType = JdbcType.TIMESTAMP)
    private LocalDateTime updatedAt;
    /**
     * 删除时间
     */
    @TableField(fill = FieldFill.UPDATE, jdbcType = JdbcType.TIMESTAMP)
    private LocalDateTime deletedAt;
    /**
     * 是否删除
     */
    @TableLogic
    private Boolean deleted;
}
