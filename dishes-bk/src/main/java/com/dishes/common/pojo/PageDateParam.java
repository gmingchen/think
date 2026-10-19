package com.dishes.common.pojo;

import com.dishes.common.constant.Constant;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 分页日期区间通用请求参数
 * @author 好市民
 */
@Data
public class PageDateParam extends PageParam {
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

