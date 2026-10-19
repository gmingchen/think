package com.dishes.common.pojo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建时间、更新时间实体对象
 * @author 好市民
 */
@Data
public class DateResult implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}
