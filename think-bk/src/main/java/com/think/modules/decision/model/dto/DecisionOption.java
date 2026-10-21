package com.think.modules.decision.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 决策单备选方案项（对应 decision.options JSON 数组元素）
 * <p>
 * options 发布后锁定，形如 {@code [{"label":"接offer"},{"label":"留下"}]}。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DecisionOption implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 方案文案
     */
    private String label;
}
