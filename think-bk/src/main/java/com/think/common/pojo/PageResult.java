package com.think.common.pojo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 分页通用返回
 * @author 好市民
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 当前页
     */
    private Long current;
    /**
     * 总记录数
     */
    private Long total;
    /**
     * 总页数
     */
    private Long pages;
    /**
     * 记录
     */
    private List<T> list;
    /**
     * 标识
     */
    private String requestId;

    public PageResult(Long total) {
        this.total = 0L;
        this.pages = 0L;
        this.list = list;
    }

    public PageResult(Long total, Long pages, List<T> list) {
        this.total = total;
        this.pages = pages;
        this.list = list;
    }
}
