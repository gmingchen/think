package com.think.core.validator.constraints;

import com.think.common.enums.EnumIntArray;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 枚举校验
 * @author 好市民
 */
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { EnumValidator.class })
public @interface Enum {

    Class<? extends EnumIntArray> value();

    String message() default "必须在指定范围 {scope}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
