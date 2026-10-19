package com.dishes.common.pojo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 分页请求参数
 * @author 好市民
 */
@Data
public class PageParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Long CURRENT = 1L;
    private static final Long SIZE = 10L;
    /**
     * 当前页
     */
    @NotNull(message = "页码不能为空")
    private Long current = CURRENT;
    /**
     * 分页大小
     */
    @NotNull(message = "分页大小不能为空")
    private Long size = SIZE;
}
