package com.dishes.common.pojo;

import com.dishes.common.constant.Constant;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 时间区间请求参数
 * @author 好市民
 */
@Data
public class DateParam implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = Constant.DATETIME_PATTERN)
    private LocalDateTime start;
    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = Constant.DATETIME_PATTERN)
    private LocalDateTime end;
}
