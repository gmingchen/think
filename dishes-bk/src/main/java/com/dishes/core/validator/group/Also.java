package com.dishes.core.validator.group;

import javax.validation.GroupSequence;

/**
 * 校验顺序 并且关系
 * @author 好市民
 */
@GroupSequence({Create.class, Update.class})
public interface Also {
}
